package org.dema.outbox

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.Timestamp
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Testcontainers
class OutboxPublisherIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17.5-alpine")
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

        store = OutboxStore(jdbc)
        kafka = mockk()
        val props = OutboxProperties(topic = "test.topic", batchSize = 100, maxAttempts = 5)
        publisher = OutboxPublisher(store, kafka, props)
        tx = TransactionTemplate(DataSourceTransactionManager(ds))
    }

    @Test
    fun `successful send marks row published`() {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "ProjectCreated", "{}")
        every { kafka.send(any(), any(), any()) } returns CompletableFuture.completedFuture(mockk(relaxed = true))

        tx.executeWithoutResult { publisher.publish() }

        val publishedAt = jdbc.queryForObject("select published_at from outbox_events where id = ?", Timestamp::class.java, id)
        assertThat(publishedAt != null).isEqualTo(true)
    }

    @Test
    fun `failing event does not block the next event`() {
        val bad = UUID.randomUUID()
        val good = UUID.randomUUID()
        store.insert(bad, "Project", UUID.randomUUID(), "Bad", """{"k":"bad"}""")
        Thread.sleep(5)
        store.insert(good, "Project", UUID.randomUUID(), "Good", """{"k":"good"}""")

        every { kafka.send("test.topic", any(), any()) } answers {
            val payloadArg = thirdArg<String>()
            if (payloadArg.contains("bad")) throw RuntimeException("boom")
            CompletableFuture.completedFuture(mockk(relaxed = true))
        }

        tx.executeWithoutResult { publisher.publish() }

        val badAttempts = jdbc.queryForObject("select attempts from outbox_events where id = ?", Int::class.java, bad)
        val badError = jdbc.queryForObject("select last_error from outbox_events where id = ?", String::class.java, bad)
        val goodPublished =
            jdbc.queryForObject("select published_at is not null from outbox_events where id = ?", Boolean::class.java, good)

        assertAll {
            assertThat(badAttempts).isEqualTo(1)
            assertThat(badError).isEqualTo("boom")
            assertThat(goodPublished).isEqualTo(true)
        }
    }
}
