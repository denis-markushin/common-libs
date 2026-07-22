package org.dema.jooq.timestamps

import org.jooq.Record
import java.time.Clock

/**
 * Single entry point for populating audit timestamp columns on a jOOQ record.
 *
 * Backs both the store path, through the RecordListener, and the upsert path, through
 * the repository. Exposes the created column name so the repository can exclude it from
 * the conflict update set.
 */
class TimestampsSupport(
    private val clock: Clock,
    private val properties: TimestampsProperties,
) {

    fun apply(record: Record) =
        TimestampsApplier.apply(record, properties.createdAtColumn, properties.updatedAtColumn, clock)

    fun createdColumn(): String = properties.createdAtColumn
}
