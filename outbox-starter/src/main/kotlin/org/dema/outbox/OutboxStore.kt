package org.dema.outbox

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.util.UUID

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

    /** MUST run inside a transaction — locks rows with FOR UPDATE SKIP LOCKED. */
    fun fetchUnpublished(limit: Int, maxAttempts: Int): List<OutboxRow> =
        jdbc.query(
            """
            select id, aggregate_id, payload
            from outbox_events
            where published_at is null and attempts < ?
            order by created_at
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
            "update outbox_events set attempts = attempts + 1, last_error = ? where id = ?",
            error, id,
        )
    }

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
