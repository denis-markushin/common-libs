package org.dema.jooq.timestamps

import org.jooq.Field
import org.jooq.RecordContext
import org.jooq.RecordListener
import org.jooq.UpdatableRecord
import java.time.Clock
import java.time.LocalDateTime

/**
 * Jooq RecordListener that automatically populates audit timestamp columns on store operations.
 *
 * Behavior:
 *  - `createdAtColumn` is populated only when null (preserves explicit values, e.g. test fixtures).
 *  - `updatedAtColumn` is overwritten unconditionally on every store.
 *
 * Hooks `storeStart` rather than `insertStart`/`updateStart` because the latter fire AFTER jOOQ
 * has already built the UPDATE SET clause from the record's `changed()` flags. Mutations from
 * `storeStart` propagate into the subsequent INSERT or UPDATE query construction.
 *
 * Triggers only on UpdatableRecord.store() — not on dsl.update(table).set(...).execute().
 *
 * @author Denis Markushin
 */
class TimestampsRecordListener(
    private val clock: Clock,
    private val createdAtColumn: String,
    private val updatedAtColumn: String,
) : RecordListener {

    override fun storeStart(ctx: RecordContext) {
        val record = ctx.record() as? UpdatableRecord<*> ?: return
        val now = LocalDateTime.now(clock)
        setIfNull(record, createdAtColumn, now)
        setAlways(record, updatedAtColumn, now)
    }

    private fun setIfNull(record: UpdatableRecord<*>, columnName: String, value: LocalDateTime) {
        val field = fieldOrNull(record, columnName) ?: return
        if (record.get(field) != null) return
        record.set(field, value)
    }

    private fun setAlways(record: UpdatableRecord<*>, columnName: String, value: LocalDateTime) {
        val field = fieldOrNull(record, columnName) ?: return
        record.set(field, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun fieldOrNull(record: UpdatableRecord<*>, columnName: String): Field<LocalDateTime>? {
        val field = record.field(columnName) ?: return null
        if (field.type != LocalDateTime::class.java) return null
        return field as Field<LocalDateTime>
    }
}
