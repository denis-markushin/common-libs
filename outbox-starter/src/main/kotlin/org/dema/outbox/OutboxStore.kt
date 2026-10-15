package org.dema.outbox

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Unpublished events split into the ones the relay still owes and the dead ones, plus how long the oldest owed one waits. */
internal data class Backlog(
    val pending: Long,
    val dead: Long,
    val age: Duration?,
)

internal data class OutboxRow(
    val id: UUID,
    val aggregateId: UUID,
    val payload: String,
)

/** JdbcTemplate persistence for the outbox_events table. */
internal class OutboxStore(
    private val jdbc: JdbcTemplate,
) {

    fun insert(id: UUID, aggregateType: String, aggregateId: UUID, eventType: String, payloadJson: String) {
        jdbc.update(
            """
            insert into outbox_events (id, aggregate_id, aggregate_type, event_type, payload)
            values (?, ?, ?, ?, cast(? as jsonb))
            """.trimIndent(),
            id, aggregateId, aggregateType, eventType, payloadJson,
        )
    }

    /**
     * MUST run inside a transaction — takes the relay lock until the transaction ends;
     * false while another relay holds it. The two-key form keyed by the outbox_events
     * table never meets the single-key locks applications take on their own ids, and
     * services sharing a database with their own tables do not wait for each other.
     * Shortens TCP keepalive of the session first, so the server drops the lock of a
     * host that vanished mid-batch within about 30 s instead of hours.
     */
    fun lock(): Boolean {
        jdbc.execute(
            """
            set local tcp_keepalives_idle = 10;
            set local tcp_keepalives_interval = 5;
            set local tcp_keepalives_count = 3;
            set local tcp_user_timeout = 30000
            """.trimIndent(),
        )
        return jdbc.queryForObject(
            "select pg_try_advisory_xact_lock('outbox_events'::regclass::oid::int, 0)",
            Boolean::class.java,
        ) == true
    }

    /** MUST run inside a transaction — locks rows with FOR UPDATE SKIP LOCKED. */
    fun fetchUnpublished(limit: Int, maxAttempts: Int): List<OutboxRow> =
        jdbc.query(
            """
            select id, aggregate_id, payload
            from outbox_events
            where published_at is null and attempts < ?
            order by seq
            limit ?
            for update skip locked
            """.trimIndent(),
            ROW_MAPPER,
            maxAttempts, limit,
        )

    fun markPublished(id: UUID) {
        jdbc.update(
            "update outbox_events set published_at = now() where id = ? and published_at is null",
            id,
        )
    }

    fun markFailed(id: UUID, error: String) {
        jdbc.update(
            "update outbox_events set attempts = attempts + 1, last_error = ? where id = ? and published_at is null",
            error, id,
        )
    }

    fun markDeferred(id: UUID, error: String) {
        jdbc.update(
            "update outbox_events set last_error = ? where id = ? and published_at is null",
            error, id,
        )
    }

    /** One pass over the unpublished rows; the age of the relay head comes from the database clock. */
    fun backlog(maxAttempts: Int): Backlog =
        jdbc.queryForObject(
            """
            select count(*) filter (where attempts < ?) as pending,
                   count(*) filter (where attempts >= ?) as dead,
                   extract(epoch from now() - (
                       select created_at
                       from outbox_events
                       where published_at is null and attempts < ?
                       order by seq
                       limit 1
                   )) as age
            from outbox_events
            where published_at is null
            """.trimIndent(),
            { rs, _ -> Backlog(rs.getLong("pending"), rs.getLong("dead"), rs.getBigDecimal("age")?.toDouble()?.seconds) },
            maxAttempts, maxAttempts, maxAttempts,
        )!!

    private companion object {
        val ROW_MAPPER = RowMapper { rs, _ ->
            OutboxRow(
                id = rs.getObject("id", UUID::class.java),
                aggregateId = rs.getObject("aggregate_id", UUID::class.java),
                payload = rs.getString("payload"),
            )
        }
    }
}
