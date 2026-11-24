package org.dema.jooq.timestamps

import org.jooq.RecordContext
import org.jooq.RecordListener

/**
 * jOOQ RecordListener that populates audit timestamp columns on store operations.
 *
 * Hooks storeStart so mutations propagate into the subsequent INSERT or UPDATE query
 * construction. Triggers only on UpdatableRecord.store(), which AbstractRepository.store()
 * reaches through batchStore; AbstractRepository covers insertOnConflictDoNothing and upsert. Delegates the column logic to TimestampsSupport.
 */
class TimestampsRecordListener(
    private val support: TimestampsSupport,
) : RecordListener {

    override fun storeStart(ctx: RecordContext) = support.apply(ctx.record())
}
