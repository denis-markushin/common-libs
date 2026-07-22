package org.dema.jooq.timestamps

import org.jooq.RecordContext
import org.jooq.RecordListener

/**
 * jOOQ RecordListener that populates audit timestamp columns on store operations.
 *
 * Hooks storeStart so mutations propagate into the subsequent INSERT or UPDATE query
 * construction. Triggers only on UpdatableRecord.store(); upsert paths are handled by
 * AbstractRepository directly. Delegates the column logic to TimestampsSupport.
 */
class TimestampsRecordListener(
    private val support: TimestampsSupport,
) : RecordListener {

    override fun storeStart(ctx: RecordContext) = support.apply(ctx.record())
}
