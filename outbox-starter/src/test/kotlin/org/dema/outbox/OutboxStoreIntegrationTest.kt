package org.dema.outbox

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
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
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID

@Testcontainers
class OutboxStoreIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17.5-alpine")
    }

    private lateinit var ds: DriverManagerDataSource
    private lateinit var jdbc: JdbcTemplate
    private lateinit var store: OutboxStore

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

        store = OutboxStore(jdbc)
    }

    @Test
    fun `insert then fetchUnpublished returns the row`() {
        val id = UUID.randomUUID()
        val aggId = UUID.randomUUID()
        store.insert(id, "Project", aggId, "ProjectCreated", """{"a":1}""")

        val rows = store.fetchUnpublished(limit = 10, maxAttempts = 5)

        assertAll {
            assertThat(rows).hasSize(1)
            assertThat(rows[0].id).isEqualTo(id)
            assertThat(rows[0].aggregateId).isEqualTo(aggId)
            assertThat(rows[0].payload).isEqualTo("""{"a": 1}""")
        }
    }

    @Test
    fun `markPublished removes row from unpublished`() {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "ProjectCreated", "{}")
        store.markPublished(id)

        assertThat(store.fetchUnpublished(10, 5)).hasSize(0)
    }

    @Test
    fun `markFailed increments attempts and dead row is excluded`() {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "ProjectCreated", "{}")
        repeat(5) { store.markFailed(id, "boom") }

        assertThat(store.fetchUnpublished(10, maxAttempts = 5)).hasSize(0)
    }

    @Test
    fun `concurrent fetchUnpublished returns disjoint rows via skip locked`() {
        // Insert 4 unpublished rows.
        val ids = (1..4).map { UUID.randomUUID() }
        ids.forEach { store.insert(it, "Project", UUID.randomUUID(), "ProjectCreated", "{}") }

        // Two independent single-connection datasources, each with its own tx boundary.
        val dsA = singleConnectionDataSource()
        val dsB = singleConnectionDataSource()
        try {
            val storeA = OutboxStore(JdbcTemplate(dsA))
            val storeB = OutboxStore(JdbcTemplate(dsB))
            val txA = TransactionTemplate(DataSourceTransactionManager(dsA))
            val txB = TransactionTemplate(DataSourceTransactionManager(dsB))

            // Tx A locks 2 rows and keeps the transaction open while B fetches.
            val idsA = txA.execute {
                val lockedByA = storeA.fetchUnpublished(limit = 2, maxAttempts = 5).map { it.id }
                // Tx B fetches the next 2 rows; skip-locked must hand it the OTHER rows.
                val idsB = txB.execute {
                    storeB.fetchUnpublished(limit = 2, maxAttempts = 5).map { it.id }
                }!!

                assertThat(lockedByA).hasSize(2)
                assertThat(idsB).hasSize(2)
                assertThat((lockedByA.toSet() + idsB.toSet())).hasSize(4)
                assertThat(lockedByA.intersect(idsB.toSet())).hasSize(0)
                lockedByA
            }!!

            assertThat(idsA).hasSize(2)
        } finally {
            dsA.destroy()
            dsB.destroy()
        }
    }

    private fun singleConnectionDataSource(): SingleConnectionDataSource =
        SingleConnectionDataSource(pg.jdbcUrl, pg.username, pg.password, true).apply {
            setAutoCommit(false)
        }
}
