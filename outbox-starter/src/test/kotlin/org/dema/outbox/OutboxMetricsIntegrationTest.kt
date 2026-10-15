package org.dema.outbox

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.apache.kafka.common.errors.RecordTooLargeException
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DelegatingDataSource
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import org.apache.kafka.common.errors.TimeoutException as KafkaTimeoutException

/** Verifies each outbox gauge reports the backlog and relay state operators alert on, without a scrape waiting for the database. */
@Testcontainers
class OutboxMetricsIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17.5-alpine")
    }

    private lateinit var ds: DriverManagerDataSource
    private lateinit var jdbc: JdbcTemplate
    private lateinit var store: OutboxStore
    private lateinit var tx: TransactionTemplate

    @BeforeEach
    fun setup() {
        ds = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)
        jdbc = JdbcTemplate(ds)
        ds.connection.use { conn ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(conn))
            Liquibase("liquibase/outbox/outbox-changelog.sql", ClassLoaderResourceAccessor(), database)
                .use { lb -> lb.update(Contexts("outbox")) }
        }
        jdbc.execute("truncate outbox_events")
        tx = TransactionTemplate(DataSourceTransactionManager(ds))
        store = OutboxStore(jdbc)
    }

    @Test
    fun `pending gauge reports the sampled backlog`() {
        val props = props()
        val metrics = OutboxMetrics(store, relay(mockk(), props), props, TestTimeSource())
        val registry = registry(metrics)
        val count = Random.nextInt(1, 12)
        repeat(count) { store.insert(UUID.randomUUID(), "Bill", UUID.randomUUID(), "BillSplit", """{"ways":$it}""") }
        metrics.sample().get(10, TimeUnit.SECONDS)
        assertThat(registry.get("outbox.events.pending").gauge().value(), "backlog gauge hides events stuck in the outbox")
            .isEqualTo(count.toDouble())
    }

    @Test
    fun `dead gauge reports events past the dead limit`() {
        val props = props()
        val metrics = OutboxMetrics(store, relay(mockk(), props), props, TestTimeSource())
        val registry = registry(metrics)
        val id = UUID.randomUUID()
        store.insert(id, "Bill", UUID.randomUUID(), "BillVoided", """{"reason":"typo"}""")
        repeat(props.maxAttempts) { store.markFailed(id, "Record of ${Random.nextInt()} bytes too large") }
        metrics.sample().get(10, TimeUnit.SECONDS)
        assertThat(registry.get("outbox.events.dead").gauge().value(), "dead gauge hides events the relay gave up on")
            .isEqualTo(1.0)
    }

    @Test
    fun `oldest age gauge reports the stuck head in seconds`() {
        val props = props()
        val metrics = OutboxMetrics(store, relay(mockk(), props), props, TestTimeSource())
        val registry = registry(metrics)
        val wait = Random.nextLong(120, 86_400)
        stuck(wait)
        metrics.sample().get(10, TimeUnit.SECONDS)
        val age = registry.get("outbox.events.oldest.age").timeGauge().value(TimeUnit.SECONDS).toLong()
        assertThat(age, "age gauge hides how long the head waits").isBetween(wait, wait + 5)
    }

    @Test
    @Timeout(10)
    fun `scrape returns the last backlog while the database hangs`() {
        val props = props()
        val source = DelegatingDataSource(ds)
        val metrics = OutboxMetrics(OutboxStore(JdbcTemplate(source)), relay(mockk(), props), props, TestTimeSource())
        val registry = registry(metrics)
        val count = Random.nextInt(1, 12)
        repeat(count) { store.insert(UUID.randomUUID(), "Bill", UUID.randomUUID(), "BillSplit", """{"ways":-$it}""") }
        metrics.sample().get(10, TimeUnit.SECONDS)
        source.targetDataSource = silent()
        assertThat(registry.get("outbox.events.pending").gauge().value(), "scrape waited on a hung database instead of the last sample")
            .isEqualTo(count.toDouble())
    }

    @Test
    fun `oldest age gauge keeps growing while the database is down`() {
        val props = props()
        val source = DelegatingDataSource(ds)
        val time = TestTimeSource()
        val metrics = OutboxMetrics(OutboxStore(JdbcTemplate(source)), relay(mockk(), props), props, time)
        val registry = registry(metrics)
        val wait = Random.nextLong(120, 86_400)
        val outage = Random.nextLong(30, 7_200)
        stuck(wait)
        metrics.sample().get(10, TimeUnit.SECONDS)
        source.targetDataSource = refused()
        time += outage.seconds
        metrics.sample().get(10, TimeUnit.SECONDS)
        val age = registry.get("outbox.events.oldest.age").timeGauge().value(TimeUnit.SECONDS).toLong()
        assertThat(age, "database outage froze the age of a stuck head").isBetween(wait + outage, wait + outage + 5)
    }

    @Test
    fun `scrape refreshes the backlog in the background`() {
        val props = props()
        val registry = registry(OutboxMetrics(store, relay(mockk(), props), props, TestTimeSource()))
        val count = Random.nextInt(1, 12)
        repeat(count) { store.insert(UUID.randomUUID(), "Bill", UUID.randomUUID(), "BillSplit", """{"ways":$it}""") }
        await().atMost(Duration.ofSeconds(10)).untilAsserted {
            assertThat(registry.get("outbox.events.pending").gauge().value(), "scrapes never refreshed the backlog")
                .isEqualTo(count.toDouble())
        }
    }

    @Test
    fun `binding samples the backlog before the first scrape`() {
        val touched = CountDownLatch(1)
        val source = object : DelegatingDataSource(ds) {
            override fun getConnection(): Connection = super.getConnection().also { touched.countDown() }
        }
        val props = props()
        registry(OutboxMetrics(OutboxStore(JdbcTemplate(source)), relay(mockk(), props), props, TestTimeSource()))
        assertThat(touched.await(10, TimeUnit.SECONDS), "first scrape after startup found no sample").isTrue()
    }

    @Test
    fun `closed metrics stop reporting the backlog`() {
        val props = props()
        val metrics = OutboxMetrics(store, relay(mockk(), props), props, TestTimeSource())
        val gauge = registry(metrics).get("outbox.events.pending").gauge()
        repeat(Random.nextInt(1, 12)) { store.insert(UUID.randomUUID(), "Bill", UUID.randomUUID(), "BillSplit", """{"ways":$it}""") }
        metrics.sample().get(10, TimeUnit.SECONDS)
        metrics.close()
        repeat(Random.nextInt(1, 4)) { gauge.value() }
        assertThat(gauge.value().isNaN(), "closed metrics kept reporting a backlog nobody samples").isTrue()
    }

    @Test
    fun `paused gauge reports a relay paused by a failed poll`() {
        store.insert(UUID.randomUUID(), "Bill", UUID.randomUUID(), "BillPaid", """{"tip":${Random.nextInt(0, 50)}}""")
        val kafka = mockk<KafkaTemplate<String, String>>()
        every { kafka.send(any(), any(), any()) } throws KafkaException("Send failed", RecordTooLargeException("too large"))
        val props = props()
        val relay = relay(kafka, props)
        relay.tick()
        val registry = registry(OutboxMetrics(store, relay, props, TestTimeSource()))
        assertThat(registry.get("outbox.relay.paused").gauge().value(), "paused relay reported as healthy").isEqualTo(1.0)
    }

    @Test
    fun `paused gauge clears once a poll goes through`() {
        store.insert(UUID.randomUUID(), "Bill", UUID.randomUUID(), "BillPaid", """{"tip":${Random.nextInt(0, 50)}}""")
        val kafka = mockk<KafkaTemplate<String, String>>()
        every { kafka.send(any(), any(), any()) } throws KafkaException("Send failed", KafkaTimeoutException("Expiring 1 record(s)"))
        val props = props()
        val time = TestTimeSource()
        val relay = OutboxPublisher(store, kafka, props, tx, time)
        relay.tick()
        every { kafka.send(any(), any(), any()) } returns CompletableFuture.completedFuture(mockk(relaxed = true))
        time += props.maxBackoffMs.milliseconds
        relay.tick()
        val registry = registry(OutboxMetrics(store, relay, props, TestTimeSource()))
        assertThat(registry.get("outbox.relay.paused").gauge().value(), "recovered relay still reported as paused").isEqualTo(0.0)
    }

    private fun props(): OutboxProperties =
        OutboxProperties(
            topic = "bills.v${Random.nextInt(1, 9)}",
            pollIntervalMs = Random.nextLong(500, 5_000),
            batchSize = Random.nextInt(5, 50),
            maxAttempts = Random.nextInt(1, 7),
            sendTimeoutMs = Random.nextLong(1_000, 9_000),
            maxBackoffMs = Random.nextLong(10_000, 90_000),
        )

    private fun relay(kafka: KafkaTemplate<String, String>, props: OutboxProperties): OutboxPublisher =
        OutboxPublisher(store, kafka, props, tx, TestTimeSource())

    private fun registry(metrics: OutboxMetrics): SimpleMeterRegistry =
        SimpleMeterRegistry().also(metrics::bindTo)

    private fun stuck(wait: Long) {
        jdbc.update(
            "insert into outbox_events (id, aggregate_id, aggregate_type, event_type, payload, created_at) " +
                "values (?, ?, 'Bill', 'BillPaid', '{}'::jsonb, now() - make_interval(secs => ?))",
            UUID.randomUUID(), UUID.randomUUID(), wait,
        )
    }

    private fun silent(): DataSource =
        object : DelegatingDataSource(ds) {
            override fun getConnection(): Connection {
                CountDownLatch(1).await()
                error("Database at ${pg.jdbcUrl} answered after hanging forever")
            }
        }

    private fun refused(): DataSource =
        object : DelegatingDataSource(ds) {
            override fun getConnection(): Connection = throw SQLException("Connection to ${pg.jdbcUrl} refused")
        }
}
