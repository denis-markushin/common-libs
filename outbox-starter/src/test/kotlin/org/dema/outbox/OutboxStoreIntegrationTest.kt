package org.dema.outbox

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@Testcontainers
class OutboxStoreIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17.5-alpine")
    }

    @Test
    fun `insert then fetchUnpublished returns the row`() = outbox {
        val id = UUID.randomUUID()
        val aggId = UUID.randomUUID()
        store.insert(id, "Project", aggId, "ProjectCreated", """{"a":1}""")
        assertThat(store.fetchUnpublished(limit = 10, maxAttempts = 5), "stored event came back changed or not at all")
            .containsExactly(OutboxRow(id, aggId, """{"a": 1}"""))
    }

    @Test
    fun `markPublished removes row from unpublished`() = outbox {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "ProjectCreated", "{}")
        store.markPublished(id)
        assertThat(store.fetchUnpublished(10, 5)).hasSize(0)
    }

    @Test
    fun `markFailed increments attempts and dead row is excluded`() = outbox {
        val id = UUID.randomUUID()
        store.insert(id, "Project", UUID.randomUUID(), "ProjectCreated", "{}")
        repeat(5) { store.markFailed(id, "boom") }
        assertThat(store.fetchUnpublished(10, maxAttempts = 5)).hasSize(0)
    }

    @Test
    fun `markFailed on published row does not increment attempts`() = outbox {
        val id = UUID.randomUUID()
        store.insert(id, "Zx9", UUID.randomUUID(), "WeirdEvt", """{"n":-17}""")
        store.markPublished(id)
        store.markFailed(id, "late failure after successful send")
        val attempts = jdbc.queryForObject("select attempts from outbox_events where id = ?", Int::class.java, id)
        assertThat(attempts).isEqualTo(0)
    }

    @Test
    fun `markDeferred does not count an attempt`() = outbox {
        val id = UUID.randomUUID()
        store.insert(id, "Kx", UUID.randomUUID(), "Deferred", """{"q":3}""")
        repeat(4) { store.markDeferred(id, "Topic kx.v1 not present in metadata after 517 ms.") }
        val attempts = jdbc.queryForObject("select attempts from outbox_events where id = ?", Int::class.java, id)
        assertThat(attempts, "deferred send counted toward the dead limit").isEqualTo(0)
    }

    @Test
    fun `markDeferred records the last error`() = outbox {
        val id = UUID.randomUUID()
        store.insert(id, "Kx", UUID.randomUUID(), "Deferred", """{"q":-8}""")
        store.markDeferred(id, "Disconnected from node 3")
        val error = jdbc.queryForObject("select last_error from outbox_events where id = ?", String::class.java, id)
        assertThat(error, "deferred send left no trace of why the row is stuck").isEqualTo("Disconnected from node 3")
    }

    @Test
    fun `markDeferred on published row keeps the error empty`() = outbox {
        val id = UUID.randomUUID()
        store.insert(id, "Kx", UUID.randomUUID(), "Deferred", """{"q":0.5}""")
        store.markPublished(id)
        store.markDeferred(id, "late timeout after a successful send")
        val error = jdbc.queryForObject("select last_error from outbox_events where id = ?", String::class.java, id)
        assertThat(error, "published row reported as stuck").isNull()
    }

    @Test
    fun `concurrent fetchUnpublished returns disjoint rows via skip locked`() = outbox {
        val ids = List(4) { UUID.randomUUID() }.onEach { store.insert(it, "Project", UUID.randomUUID(), "ProjectCreated", "{}") }
        val fetched = connection().use { first ->
            connection().use { second ->
                val a = SingleConnectionDataSource(first, true)
                val b = SingleConnectionDataSource(second, true)
                TransactionTemplate(DataSourceTransactionManager(a)).execute {
                    OutboxStore(JdbcTemplate(a)).fetchUnpublished(limit = 2, maxAttempts = 5).map { it.id } +
                        TransactionTemplate(DataSourceTransactionManager(b)).execute {
                            OutboxStore(JdbcTemplate(b)).fetchUnpublished(limit = 2, maxAttempts = 5).map { it.id }
                        }
                }
            }
        }
        assertThat(fetched, "two relays fetched the same locked row").containsExactlyInAnyOrder(*ids.toTypedArray())
    }

    @Test
    fun `fetchUnpublished orders by insertion sequence not created_at`() = outbox {
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()
        jdbc.update(
            "insert into outbox_events (id, aggregate_id, aggregate_type, event_type, payload, created_at) " +
                "values (?, ?, 'Q', 'Later', '{}'::jsonb, now() + interval '1 hour')",
            first, UUID.randomUUID(),
        )
        jdbc.update(
            "insert into outbox_events (id, aggregate_id, aggregate_type, event_type, payload, created_at) " +
                "values (?, ?, 'Q', 'Earlier', '{}'::jsonb, now())",
            second, UUID.randomUUID(),
        )
        assertThat(store.fetchUnpublished(10, 5).map { it.id }).containsExactly(first, second)
    }

    @Test
    fun `application lock on an id equal to the table oid leaves the relay lock free`() = outbox {
        var free: Boolean? = null
        TransactionTemplate(DataSourceTransactionManager(source)).executeWithoutResult {
            jdbc.execute("select pg_advisory_xact_lock('outbox_events'::regclass::oid::bigint)")
            free = lock()
        }
        assertThat(free, "application lock on an entity id stalled the relay").isEqualTo(true)
    }

    @Test
    fun `lock bounds how long a vanished holder keeps it`() = outbox {
        var keepalive: String? = null
        lock {
            keepalive = it.queryForObject(
                """
                select concat_ws(',', current_setting('tcp_keepalives_idle'), current_setting('tcp_keepalives_interval'),
                    current_setting('tcp_keepalives_count'), current_setting('tcp_user_timeout'))
                """.trimIndent(),
                String::class.java,
            )
        }
        assertThat(keepalive, "vanished holder would keep the relay lock for hours").isEqualTo("10,5,3,30000")
    }

    @Test
    fun `lock is refused while another transaction holds it`() = outbox {
        var rival: Boolean? = null
        lock { rival = lock() }
        assertThat(rival, "two transactions held the relay lock at once").isEqualTo(false)
    }

    @Test
    fun `lock is free again once its transaction ends`() = outbox {
        val free = connection().use { connection ->
            val source = SingleConnectionDataSource(connection, true)
            TransactionTemplate(DataSourceTransactionManager(source)).execute { OutboxStore(JdbcTemplate(source)).lock() }
            lock()
        }
        assertThat(free, "relay lock outlived the transaction that took it").isTrue()
    }

    @Test
    fun `lock of one outbox table leaves the table of another schema free`() = outbox {
        val schema = "outbox_${UUID.randomUUID().toString().replace("-", "")}"
        jdbc.execute("create schema $schema")
        jdbc.execute("create table $schema.outbox_events (like public.outbox_events)")
        var other: Boolean? = null
        lock { other = lock(schema) }
        assertThat(other, "relay lock of one outbox table blocked another").isEqualTo(true)
    }

    @Test
    fun `backlog counts pending events below the dead limit`() = outbox {
        val limit = Random.nextInt(1, 6)
        val count = Random.nextInt(2, 9)
        repeat(count) { event(Random.nextLong(0, 900)) }
        event(Random.nextLong(0, 900)).also { id -> repeat(limit) { store.markFailed(id, "Record of ${Random.nextInt()} bytes") } }
        store.markPublished(event(Random.nextLong(0, 900)))
        assertThat(store.backlog(limit).pending, "backlog counted events the relay no longer owes").isEqualTo(count.toLong())
    }

    @Test
    fun `backlog counts dead events at the dead limit`() = outbox {
        val limit = Random.nextInt(1, 6)
        val count = Random.nextInt(1, 5)
        repeat(count) { event(Random.nextLong(0, 900)).also { id -> repeat(limit) { store.markFailed(id, "Serialization failed") } } }
        event(Random.nextLong(0, 900))
        event(Random.nextLong(0, 900))
            .also { id -> repeat(limit) { store.markFailed(id, "Serialization failed") } }
            .also(store::markPublished)
        assertThat(store.backlog(limit).dead, "dead count missed or invented events").isEqualTo(count.toLong())
    }

    @Test
    fun `backlog measures how long the oldest pending event waits`() = outbox {
        val wait = Random.nextLong(120, 86_400)
        event(wait)
        event(Random.nextLong(0, 100))
        val age = store.backlog(Random.nextInt(1, 6)).age?.inWholeSeconds
        assertThat(age, "stuck head looks fresher than it is").isNotNull().isBetween(wait, wait + 5)
    }

    @Test
    fun `backlog age skips a dead event ahead of the pending ones`() = outbox {
        val limit = Random.nextInt(1, 6)
        val wait = Random.nextLong(120, 86_400)
        event(wait * 2).also { id -> repeat(limit) { store.markFailed(id, "Invalid record") } }
        event(wait)
        assertThat(store.backlog(limit).age?.inWholeSeconds, "dead event kept aging the relay head").isNotNull().isBetween(wait, wait + 5)
    }

    @Test
    fun `backlog has no age without pending events`() = outbox {
        store.markPublished(event(Random.nextLong(60, 86_400)))
        assertThat(store.backlog(Random.nextInt(1, 6)).age, "drained outbox reported a stuck head").isNull()
    }

    @Test
    fun `purge deletes events published before the retention`() = outbox {
        val retention = Random.nextLong(60, 86_400)
        published(retention + Random.nextLong(5, 3_600))
        store.purge(retention.seconds, Random.nextInt(1, 50))
        assertThat(rows(), "event outlived its retention").isEqualTo(0)
    }

    @Test
    fun `purge keeps unpublished events however old`() = outbox {
        event(Random.nextLong(86_400, 9_000_000))
        store.purge(Random.nextLong(1, 60).seconds, Random.nextInt(1, 50))
        assertThat(rows(), "purge deleted an event the relay still owes").isEqualTo(1)
    }

    @Test
    fun `purge keeps events published within the retention`() = outbox {
        val retention = Random.nextLong(600, 86_400)
        published(Random.nextLong(0, retention - 60))
        store.purge(retention.seconds, Random.nextInt(1, 50))
        assertThat(rows(), "purge deleted an event still within its retention").isEqualTo(1)
    }

    @Test
    fun `purge deletes no more than the limit`() = outbox {
        val limit = Random.nextInt(1, 6)
        val count = limit + Random.nextInt(1, 6)
        repeat(count) { published(Random.nextLong(7_200, 86_400)) }
        store.purge(Random.nextLong(60, 3_600).seconds, limit)
        assertThat(rows(), "one purge statement deleted more than its limit").isEqualTo(count - limit)
    }

    private fun Outbox.published(age: Long): UUID =
        UUID.randomUUID().also { id ->
            jdbc.update(
                "insert into outbox_events (id, aggregate_id, aggregate_type, event_type, payload, created_at, published_at) " +
                    "values (?, ?, 'Till', 'Closed', '{}'::jsonb, now() - make_interval(secs => ?), now() - make_interval(secs => ?))",
                id, UUID.randomUUID(), age + 1, age,
            )
        }

    private fun Outbox.rows(): Int? = jdbc.queryForObject("select count(*) from outbox_events", Int::class.java)

    private fun Outbox.event(age: Long): UUID =
        UUID.randomUUID().also { id ->
            jdbc.update(
                "insert into outbox_events (id, aggregate_id, aggregate_type, event_type, payload, created_at) " +
                    "values (?, ?, 'Till', 'Opened', '{}'::jsonb, now() - make_interval(secs => ?))",
                id, UUID.randomUUID(), age,
            )
        }

    private fun Outbox.lock(schema: String = "public", during: (JdbcTemplate) -> Unit = {}): Boolean {
        val source = session()
        val session = JdbcTemplate(source)
        return TransactionTemplate(DataSourceTransactionManager(source)).execute {
            session.execute("set local search_path = $schema")
            OutboxStore(session).lock().also { if (it) during(session) }
        } == true
    }

    private fun outbox(block: Outbox.() -> Unit) = Outbox(pg).block()
}
