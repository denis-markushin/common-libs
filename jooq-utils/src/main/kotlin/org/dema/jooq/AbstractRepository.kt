package org.dema.jooq

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.Table
import org.jooq.UpdatableRecord
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractRepository<T : Table<R>, R : UpdatableRecord<*>>(
    protected val table: T,
    private val baseCondition: Condition = DSL.noCondition(),
) {
    @Autowired
    protected lateinit var dsl: DSLContext

    fun newRec(): R {
        return dsl.newRecord(table)
    }

    fun insertOnConflictDoNothing(record: R): Int {
        return dsl.insertInto(table).set(record).onConflictDoNothing().execute()
    }

    fun store(vararg records: R) {
        dsl.batchStore(*records).execute()
    }

    fun store(records: Collection<R>) {
        dsl.batchStore(records).execute()
    }

    fun deleteBy(vararg where: (T) -> Condition): Int {
        return dsl.deleteFrom(table).where(foldConditions(where)).execute()
    }

    fun getOneBy(where: (T) -> Condition): R? {
        return dsl.selectFrom(table).where(baseCondition.and(where.invoke(table))).fetchOne()
    }

    fun getAllBy(where: (T) -> Condition): Result<R> {
        return dsl.selectFrom(table).where(baseCondition.and(where.invoke(table))).fetch()
    }

    private fun foldConditions(where: Array<out (T) -> Condition>) = where.fold(DSL.noCondition()) { acc, func -> acc.and(func(table)) }
}