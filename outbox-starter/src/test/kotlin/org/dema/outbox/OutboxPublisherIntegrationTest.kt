package org.dema.outbox

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import io.mockk.every
import io.mockk.mockk
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.InvalidRecordException
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.common.errors.ClusterAuthorizationException
import org.apache.kafka.common.errors.InterruptException
import org.apache.kafka.common.errors.RecordBatchTooLargeException
import org.apache.kafka.common.errors.RecordTooLargeException
import org.apache.kafka.common.errors.SaslAuthenticationException
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.errors.TopicAuthorizationException
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import java.net.ServerSocket
import java.sql.Timestamp
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource
import org.apache.kafka.common.errors.TimeoutException as KafkaTimeoutException

@Testcontainers
class OutboxPublisherIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17.5-alpine")

        @Container
        @JvmStatic
        val broker = KafkaContainer("apache/kafka-native:3.8.0")
    }

    private lateinit var jdbc: JdbcTemplate
    private lateinit var store: OutboxStore
    private lateinit var kafka: KafkaTemplate<String, String>
    private lateinit var publisher: OutboxPublisher
    private lateinit var tx: TransactionTemplate

    @BeforeEach
    fun setup() {
        val ds = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)
        jdbc = JdbcTemplate(ds)

        // Apply the real shipped changelog (idempotent: Liquibase skips applied changesets).
        ds.connection.use { conn ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(conn))
            Liquibase("liquibase/outbox/outbox-changelog.sql", ClassLoaderResourceAccessor(), database)
                .use { lb -> lb.update(Contexts("outbox")) }
        }

        // Container is reused across tests; clear rows for isolation.
        jdbc.execute("truncate outbox_events")

        tx = TransactionTemplate(DataSourceTransactionManager(ds))
        store = OutboxStore(jdbc)
        kafka = mockk()
        publisher = OutboxPublisher(store, kafka, props("test.topic", attempts = 5, timeout = 250), tx, TimeSource.Monotonic)
    }

    @Test
    fun `successful send marks row published`() {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "ProjectCreated", "{}")
        every { kafka.send(any(), any(), any()) } returns CompletableFuture.completedFuture(mockk(relaxed = true))

        publisher.publish()

        val publishedAt = jdbc.queryForObject("select published_at from outbox_events where id = ?", Timestamp::class.java, id)
        assertThat(publishedAt != null).isEqualTo(true)
    }

    @Test
    fun `send exceeding timeout records the timeout`() {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "SlowEvt", """{"slow":true}""")
        every { kafka.send(any(), any(), any()) } returns hang(TimeoutException())
        publisher.publish()
        assertThat(failure(id), "stuck event hides that the broker never answered").isEqualTo("Kafka send timed out after 250 ms")
    }

    @Test
    fun `send timeout does not count an attempt`() {
        val id = UUID.randomUUID()
        store.insert(id, "Ledger", UUID.randomUUID(), "Posted", """{"amount":-31}""")
        every { kafka.send(any(), any(), any()) } returns hang(TimeoutException())
        publisher.publish()
        assertThat(attempts(id), "slow broker counted against a healthy event").isEqualTo(0)
    }

    @Test
    fun `async failure records root cause not the wrapper`() {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "AsyncFail", """{"x":"y"}""")
        val failed = CompletableFuture<SendResult<String, String>>()
        failed.completeExceptionally(RuntimeException("broker unreachable: node -3"))
        every { kafka.send(any(), any(), any()) } returns failed
        publisher.publish()
        val lastError = jdbc.queryForObject("select last_error from outbox_events where id = ?", String::class.java, id)
        assertThat(lastError).isEqualTo("broker unreachable: node -3")
    }

    @Test
    fun `interrupted send restores the interrupt flag`() {
        store.insert(UUID.randomUUID(), "Project", UUID.randomUUID(), "IntEvt", "{}")
        every { kafka.send(any(), any(), any()) } returns hang(InterruptedException())
        publisher.publish()
        val interrupted = Thread.interrupted()
        assertThat(interrupted).isEqualTo(true)
    }

    @Test
    fun `interrupted send does not count an attempt`() {
        val id = UUID.randomUUID()
        store.insert(id, "Shift", UUID.randomUUID(), "Closed", """{"till":4}""")
        every { kafka.send(any(), any(), any()) } returns hang(InterruptedException())
        publisher.publish()
        Thread.interrupted()
        assertThat(attempts(id), "shutdown counted against a healthy event").isEqualTo(0)
    }

    @Test
    fun `producer interrupted while blocked does not count an attempt`() {
        val id = UUID.randomUUID()
        store.insert(id, "Shift", UUID.randomUUID(), "Opened", """{"till":-9}""")
        every { kafka.send(any(), any(), any()) } answers { throw InterruptException(InterruptedException()) }
        publisher.publish()
        Thread.interrupted()
        assertThat(attempts(id), "rolling deploy during an outage counted against a healthy event").isEqualTo(0)
    }

    @ParameterizedTest
    @ValueSource(
        classes = [
            ConfigException::class,
            SaslAuthenticationException::class,
            TopicAuthorizationException::class,
            ClusterAuthorizationException::class,
        ],
    )
    fun `failure outside the event does not count an attempt`(fault: Class<out Exception>) {
        val id = UUID.randomUUID()
        store.insert(id, "Order", UUID.randomUUID(), "OrderClosed", """{"guests":${Random.nextInt(1, 40)}}""")
        val cause = fault.getConstructor(String::class.java).newInstance("kafka-${Random.nextInt()} refused")
        every { kafka.send(any(), any(), any()) } throws KafkaException("Failed to construct kafka producer", cause)
        publisher.publish()
        assertThat(attempts(id), "${fault.simpleName} counted against a healthy event").isEqualTo(0)
    }

    @ParameterizedTest
    @ValueSource(
        classes = [
            RecordTooLargeException::class,
            RecordBatchTooLargeException::class,
            SerializationException::class,
            InvalidRecordException::class,
        ],
    )
    fun `failure of the event itself counts an attempt`(fault: Class<out Exception>) {
        val id = UUID.randomUUID()
        store.insert(id, "Order", UUID.randomUUID(), "OrderClosed", """{"guests":${Random.nextInt(1, 40)}}""")
        val cause = fault.getConstructor(String::class.java).newInstance("record ${Random.nextInt()} rejected")
        every { kafka.send(any(), any(), any()) } throws KafkaException("Send failed", cause)
        publisher.publish()
        assertThat(attempts(id), "${fault.simpleName} never counted toward the dead limit").isEqualTo(1)
    }

    @Test
    fun `missing topic records the topic not the partition error`() {
        val id = UUID.randomUUID()
        store.insert(id, "Order", UUID.randomUUID(), "OrderClosed", """{"total":64}""")
        val metadata = KafkaTimeoutException(
            "Topic orders.v7 not present in metadata after 5000 ms.",
            UnknownTopicOrPartitionException("This server does not host this topic-partition."),
        )
        every { kafka.send(any(), any(), any()) } throws KafkaException("Send failed", metadata)
        publisher.publish()
        assertThat(failure(id), "stuck event hides which topic is missing").isNotNull().contains("orders.v7")
    }

    @Test
    fun `transient failure keeps events of one aggregate in insertion order`() {
        val aggregate = UUID.randomUUID()
        val payloads = List(3) { """{"n": ${Random.nextInt()}}""" }
        payloads.forEach { store.insert(UUID.randomUUID(), "Order", aggregate, "OrderClosed", it) }
        val wire = mutableListOf<String>()
        var outage = true
        every { kafka.send(any(), any(), any()) } answers {
            if (outage) {
                outage = false
                throw KafkaTimeoutException("Expiring 1 record(s)")
            }
            wire += thirdArg<String>()
            CompletableFuture.completedFuture(mockk(relaxed = true))
        }
        repeat(2) { publisher.publish() }
        assertThat(wire, "a later event overtook the one that failed").isEqualTo(payloads)
    }

    @Test
    fun `relay skips polls while paused after a failure`() {
        val id = UUID.randomUUID()
        store.insert(id, "Order", UUID.randomUUID(), "OrderClosed", """{"seat":12}""")
        every { kafka.send(any(), any(), any()) } throws rejection()
        val props = OutboxProperties(
            topic = "orders.v1",
            pollIntervalMs = 7_300,
            batchSize = 29,
            maxAttempts = 9,
            sendTimeoutMs = 3_100,
            maxBackoffMs = 90_000,
        )
        val relay = OutboxPublisher(store, kafka, props, tx, TestTimeSource())
        repeat(3) { relay.tick() }
        assertThat(attempts(id), "relay hammered the broker instead of pausing").isEqualTo(1)
    }

    @Test
    fun `relay polls again once the pause elapses`() {
        val id = UUID.randomUUID()
        store.insert(id, "Order", UUID.randomUUID(), "OrderClosed", """{"seat":-2}""")
        every { kafka.send(any(), any(), any()) } throws rejection()
        val props = OutboxProperties(
            topic = "orders.v1",
            pollIntervalMs = 4_100,
            batchSize = 13,
            maxAttempts = 9,
            sendTimeoutMs = 2_700,
            maxBackoffMs = 50_000,
        )
        val time = TestTimeSource()
        val relay = OutboxPublisher(store, kafka, props, tx, time)
        relay.tick()
        time += 4_100.milliseconds
        relay.tick()
        assertThat(attempts(id), "relay never resumed after its pause").isEqualTo(2)
    }

    @Test
    fun `manual publish leaves the relay pause in place`() {
        val tail = UUID.randomUUID()
        store.insert(UUID.randomUUID(), "Order", UUID.randomUUID(), "OrderClosed", """{"seat":31}""")
        every { kafka.send(any(), any(), any()) } throws rejection()
        val props = OutboxProperties(
            topic = "orders.v1",
            pollIntervalMs = 6_200,
            batchSize = 17,
            maxAttempts = 1,
            sendTimeoutMs = 1_900,
            maxBackoffMs = 70_000,
        )
        val relay = OutboxPublisher(store, kafka, props, tx, TestTimeSource())
        relay.tick()
        every { kafka.send(any(), any(), any()) } returns CompletableFuture.completedFuture(mockk(relaxed = true))
        relay.publish()
        store.insert(tail, "Order", UUID.randomUUID(), "OrderCancelled", """{"seat":31}""")
        relay.tick()
        assertThat(publication(tail), "caller thread lifted the relay pause").isNull()
    }

    @Test
    fun `unreachable broker does not count an attempt`() {
        val id = UUID.randomUUID()
        store.insert(id, "Order", UUID.randomUUID(), "OrderClosed", """{"total":731}""")
        val props = props("orders.${UUID.randomUUID()}", attempts = 1, timeout = 2_000)
        producer(nowhere(), ProducerConfig.MAX_BLOCK_MS_CONFIG to 300) { relay(it, props).publish() }
        assertThat(attempts(id), "broker outage counted against a healthy event").isEqualTo(0)
    }

    @Test
    fun `unreachable broker records the root cause not the template wrapper`() {
        val id = UUID.randomUUID()
        store.insert(id, "Order", UUID.randomUUID(), "OrderClosed", """{"total":88}""")
        val props = props("orders.${UUID.randomUUID()}", attempts = 2, timeout = 2_000)
        producer(nowhere(), ProducerConfig.MAX_BLOCK_MS_CONFIG to 300) { relay(it, props).publish() }
        assertThat(failure(id), "stuck event hides which topic the broker could not serve").isNotNull().contains(props.topic)
    }

    @Test
    fun `unreachable broker stops the batch at the first event`() {
        val tail = UUID.randomUUID()
        val aggregate = UUID.randomUUID()
        store.insert(UUID.randomUUID(), "Order", aggregate, "OrderClosed", """{"total":5}""")
        store.insert(tail, "Order", aggregate, "OrderCancelled", """{"total":-5}""")
        val props = props("orders.${UUID.randomUUID()}", attempts = 3, timeout = 2_000)
        producer(nowhere(), ProducerConfig.MAX_BLOCK_MS_CONFIG to 300) { relay(it, props).publish() }
        assertThat(failure(tail), "relay kept sending into a dead broker").isNull()
    }

    @Test
    fun `events held back by an outage reach the topic in insertion order`() {
        val aggregate = UUID.randomUUID()
        val payloads = List(4) { """{"n": ${Random.nextInt()}}""" }
        payloads.forEach { store.insert(UUID.randomUUID(), "Order", aggregate, "OrderClosed", it) }
        val props = props("orders.${UUID.randomUUID()}", attempts = 1, timeout = 10_000)
        producer(nowhere(), ProducerConfig.MAX_BLOCK_MS_CONFIG to 300) { relay(it, props).publish() }
        producer(broker.bootstrapServers, ProducerConfig.MAX_BLOCK_MS_CONFIG to 10_000) { relay(it, props).publish() }
        assertThat(records(props.topic, payloads.size), "outage lost or reordered events of one aggregate")
            .isEqualTo(payloads)
    }

    @Test
    fun `oversized event counts an attempt`() {
        val id = UUID.randomUUID()
        store.insert(id, "Menu", UUID.randomUUID(), "MenuPublished", blob())
        val props = props("menus.${UUID.randomUUID()}", attempts = 4, timeout = 10_000)
        producer(broker.bootstrapServers, ProducerConfig.MAX_REQUEST_SIZE_CONFIG to 1_024) { relay(it, props).publish() }
        assertThat(attempts(id), "record too large never counted toward the dead limit").isEqualTo(1)
    }

    @Test
    fun `oversized event stops the batch`() {
        val tail = UUID.randomUUID()
        store.insert(UUID.randomUUID(), "Menu", UUID.randomUUID(), "MenuPublished", blob())
        store.insert(tail, "Menu", UUID.randomUUID(), "MenuPublished", """{"dish":"borscht"}""")
        val props = props("menus.${UUID.randomUUID()}", attempts = 4, timeout = 10_000)
        producer(broker.bootstrapServers, ProducerConfig.MAX_REQUEST_SIZE_CONFIG to 1_024) { relay(it, props).publish() }
        assertThat(publication(tail), "event overtook the failed one ahead of it").isNull()
    }

    @Test
    fun `dead event stops blocking the relay`() {
        val tail = UUID.randomUUID()
        store.insert(UUID.randomUUID(), "Menu", UUID.randomUUID(), "MenuPublished", blob())
        store.insert(tail, "Menu", UUID.randomUUID(), "MenuPublished", """{"dish":"pelmeni"}""")
        val props = props("menus.${UUID.randomUUID()}", attempts = 1, timeout = 10_000)
        producer(broker.bootstrapServers, ProducerConfig.MAX_REQUEST_SIZE_CONFIG to 1_024) { template ->
            repeat(2) { relay(template, props).publish() }
        }
        assertThat(publication(tail), "dead event kept blocking the events behind it").isNotNull()
    }

    private fun props(topic: String, attempts: Int, timeout: Long): OutboxProperties =
        OutboxProperties(
            topic = topic,
            pollIntervalMs = 1_700,
            batchSize = 37,
            maxAttempts = attempts,
            sendTimeoutMs = timeout,
            maxBackoffMs = 23_000,
        )

    private fun relay(template: KafkaTemplate<String, String>, props: OutboxProperties): OutboxPublisher =
        OutboxPublisher(store, template, props, tx, TimeSource.Monotonic)

    private fun attempts(id: UUID): Int? =
        jdbc.queryForObject("select attempts from outbox_events where id = ?", Int::class.java, id)

    private fun failure(id: UUID): String? =
        jdbc.queryForObject("select last_error from outbox_events where id = ?", String::class.java, id)

    private fun publication(id: UUID): Timestamp? =
        jdbc.queryForObject("select published_at from outbox_events where id = ?", Timestamp::class.java, id)

    private fun hang(fault: Exception): CompletableFuture<SendResult<String, String>> {
        val future = mockk<CompletableFuture<SendResult<String, String>>>()
        every { future.get(any<Long>(), any()) } throws fault
        return future
    }

    private fun rejection(): KafkaException =
        KafkaException("Send failed", RecordTooLargeException("The message is ${Random.nextInt(1_100_000, 9_000_000)} bytes"))

    private fun <T> producer(
        bootstrap: String,
        vararg config: Pair<String, Any>,
        block: (KafkaTemplate<String, String>) -> T,
    ): T {
        val factory = DefaultKafkaProducerFactory<String, String>(
            mapOf<String, Any>(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrap,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ) + config,
        )
        try {
            return block(KafkaTemplate(factory))
        } finally {
            factory.destroy()
        }
    }

    private fun records(topic: String, count: Int): List<String> =
        KafkaConsumer<String, String>(
            mapOf<String, Any>(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to broker.bootstrapServers,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ),
        ).use { consumer ->
            consumer.assign(listOf(TopicPartition(topic, 0)))
            val values = mutableListOf<String>()
            await().atMost(Duration.ofSeconds(30)).pollInSameThread().until {
                consumer.poll(Duration.ofMillis(200)).forEach { values += it.value() }
                values.size >= count
            }
            values
        }

    private fun nowhere(): String = ServerSocket(0).use { "127.0.0.1:${it.localPort}" }

    private fun blob(): String =
        """{"blob": "${(1..Random.nextInt(2_000, 4_000)).map { ('a'..'z').random() }.joinToString("")}"}"""
}
