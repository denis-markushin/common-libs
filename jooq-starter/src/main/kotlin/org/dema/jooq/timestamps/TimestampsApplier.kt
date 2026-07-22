package org.dema.jooq.timestamps

import org.jooq.Field
import org.jooq.Record
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime

/**
 * Populates audit timestamp columns on a jOOQ record.
 *
 * The created column is set only when currently null, preserving explicit values.
 * The updated column is overwritten on every call. Columns absent from the record
 * or of an unsupported type are skipped.
 */
object TimestampsApplier {

    fun apply(record: Record, createdAtColumn: String, updatedAtColumn: String, clock: Clock) {
        set(record, createdAtColumn, clock, overwrite = false)
        set(record, updatedAtColumn, clock, overwrite = true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun set(record: Record, column: String, clock: Clock, overwrite: Boolean) {
        val field = (record.field(column) ?: return) as Field<Any>
        val value = valueFor(field.type, clock) ?: return
        if (!overwrite && record.get(field) != null) return
        record.set(field, value)
    }

    private fun valueFor(type: Class<*>, clock: Clock): Any? = when (type) {
        LocalDateTime::class.java -> LocalDateTime.now(clock)
        OffsetDateTime::class.java -> OffsetDateTime.now(clock)
        Instant::class.java -> Instant.now(clock)
        else -> null
    }
}
