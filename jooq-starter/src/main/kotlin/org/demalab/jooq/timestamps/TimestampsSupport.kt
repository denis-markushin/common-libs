package org.demalab.jooq.timestamps

import org.jooq.Field
import org.jooq.Record
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime

/**
 * Single entry point for populating audit timestamp columns on a jOOQ record.
 *
 * Backs both the store path, through the RecordListener, and the repository write paths
 * the listener never sees. The created column is set only when currently null, preserving
 * explicit values; the updated column is overwritten on every call. Columns absent from
 * the record or of an unsupported type are skipped. Exposes the created column name so the
 * repository can exclude it from the conflict update set.
 */
class TimestampsSupport(
    private val clock: Clock,
    private val properties: TimestampsProperties,
) {

    fun apply(record: Record) {
        set(record, properties.createdAtColumn, overwrite = false)
        set(record, properties.updatedAtColumn, overwrite = true)
    }

    fun createdColumn(): String = properties.createdAtColumn

    @Suppress("UNCHECKED_CAST")
    private fun set(record: Record, column: String, overwrite: Boolean) {
        val field = (record.field(column) ?: return) as Field<Any>
        val value = value(field.type) ?: return
        if (!overwrite && record.get(field) != null) return
        record.set(field, value)
    }

    private fun value(type: Class<*>): Any? = when (type) {
        LocalDateTime::class.java -> LocalDateTime.now(clock)
        OffsetDateTime::class.java -> OffsetDateTime.now(clock)
        Instant::class.java -> Instant.now(clock)
        else -> null
    }
}
