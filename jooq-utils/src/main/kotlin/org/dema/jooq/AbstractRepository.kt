package org.dema.jooq

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.OrderField
import org.jooq.Result
import org.jooq.SelectConditionStep
import org.jooq.Table
import org.jooq.UpdatableRecord
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.noCondition
import org.jooq.impl.SQLDataType
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

    /**
     * Inserts or updates a given record. If a conflict on primary keys occurs, the existing row will be updated.
     *
     * Inspired by the [article](https://sigpwned.com/2023/08/10/postgres-upsert-created-or-updated)
     *
     * @param record The record to upsert.
     * @return An [UpsertResult] indicating the created or updated record.
     */
    fun upsert(record: R): UpsertResult<R> {
        return dsl.insertInto(table)
            .set(record)
            .onConflict(table.primaryKey!!.fields)
            .doUpdate()
            .setAllToExcluded()
            .returningResult(table, field("(xmax = 0)", SQLDataType.BOOLEAN).`as`("_created"))
            .fetchOne { UpsertResult(it.value1(), it.value2()) }
            ?: throw IllegalStateException("Upsert operation failed to return a record")
    }

    /**
     * Inserts the given record. If a conflict on primary keys occurs, no action is taken.
     *
     * @param record The record to insert.
     * @return The number of affected rows.
     */
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