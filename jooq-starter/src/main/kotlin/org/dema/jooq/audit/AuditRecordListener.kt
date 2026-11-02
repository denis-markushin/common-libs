package org.dema.jooq.audit

import org.jooq.RecordContext
import org.jooq.RecordListener
import org.jooq.UpdatableRecord

/**
 * jOOQ RecordListener that populates audit actor columns on store operations.
 *
 * Hooks storeStart because insertStart and updateStart fire after jOOQ has built the SET
 * clause. Triggers only on UpdatableRecord.store(); AbstractRepository covers
 * insertOnConflictDoNothing and upsert. Delegates the column logic to AuditSupport.
 */
class AuditRecordListener(
    private val support: AuditSupport,
) : RecordListener {

    override fun storeStart(ctx: RecordContext) = support.apply(ctx.record() as UpdatableRecord<*>)
}
