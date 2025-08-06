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

/**
 * Abstract base repository for working with a jOOQ table and providing common CRUD operations.
 *
 * @param <T> The type of jOOQ Table this repository works with.
 * @param <R> The type of UpdatableRecord for the corresponding table.
 *
 * @author Denis Markushin
 */
abstract class AbstractRepository<T : Table<R>, R : UpdatableRecord<*>>(
    /**
     * Represents the jOOQ table for which operations will be performed.
     */
    protected val table: T,
    /**
     * The base condition to apply for queries.
     */
    private val baseCondition: Condition = noCondition(),
) {
    @Autowired
    protected lateinit var dsl: DSLContext

    /**
     * Creates a new record for the associated table.
     *
     * @return A new instance of the record type associated with the table.
     */
    fun newRec(): R = dsl.newRecord(table)

    /**
     * Inserts or updates a given record. If a conflict on primary keys occurs, the existing row will be updated.
     *
     * Inspired by the [article](https://sigpwned.com/2023/08/10/postgres-upsert-created-or-updated)
     *
     * @param record The record to upsert.
     * @return An [UpsertResult] indicating the created or updated record.
     */
    fun upsert(record: R): UpsertResult<R> = dsl.insertInto(table)
        .set(record)
        .onConflict(table.primaryKey!!.fields)
        .doUpdate()
        .setAllToExcluded()
        .returningResult(table, field("(xmax = 0)", SQLDataType.BOOLEAN).`as`("_created"))
        .fetchOne { UpsertResult(it.value1(), it.value2()) }
        ?: throw IllegalStateException("Upsert operation failed to return a record")

    /**
     * Inserts the given record. If a conflict on primary keys occurs, no action is taken.
     *
     * @param record The record to insert.
     * @return The number of affected rows.
     */
    fun insertOnConflictDoNothing(record: R): Int = dsl.insertInto(table).set(record).onConflictDoNothing().execute()

    /**
     * Stores multiple records in batch.
     *
     * @param records Records to store.
     */
    fun store(vararg records: R) {
        dsl.batchStore(*records).execute()
    }

    /**
     * Stores multiple records in batch.
     *
     * @param records Records to store.
     */
    fun store(records: Collection<R>) {
        dsl.batchStore(records).execute()
    }

    /**
     * Deletes records based on the provided conditions.
     *
     * @param where Conditions to be applied for deletion.
     * @return Number of records deleted.
     */
    fun deleteBy(vararg where: (T) -> Condition): Int = dsl.deleteFrom(table).where(foldConditions(where)).execute()

    /**
     * Fetches a single record matching the provided condition.
     *
     * @param forUpdate Whether to lock the record for update.
     * @param where Condition to match.
     * @return The matching record, or null if none found.
     */
    fun getOneBy(
        forUpdate: Boolean = false,
        where: (T) -> Condition,
    ): R? = baseQuery()
        .and(where.invoke(table))
        .apply { if (forUpdate) forUpdate().skipLocked() }
        .fetchOne()

    /**
     * Fetches all records matching the provided condition.
     *
     * @param forUpdate Whether to lock the records for update.
     * @param where Condition to match.
     * @return A Result containing all matching records.
     */
    fun getAllBy(
        forUpdate: Boolean = false,
        where: (T) -> Condition,
    ): Result<R> = baseQuery()
        .and(where.invoke(table))
        .apply { if (forUpdate) forUpdate().skipLocked() }
        .fetch()

    /**
     * Fetches all records matching the provided condition, with support for ordering, seeking, and limiting results.
     *
     * @param orderBy The fields by which to order the result set.
     * @param seekValues The values for the seek operation to determine where to continue fetching from.
     * @param limit The maximum number of records to return.
     * @param where The condition to apply to the query.
     * @return A Result containing the matching records.
     */
    fun getAllBy(
        orderBy: Array<out OrderField<*>>,
        seekValues: Array<out Any>,
        limit: Int,
        where: (T) -> Condition,
    ): Result<R> = baseQuery()
        .and(where.invoke(table))
        .orderBy(*orderBy)
        .seek(*seekValues)
        .limit(limit)
        .fetch()

    /**
     * Creates a base query for the associated table with the base condition applied.
     *
     * @return A [SelectConditionStep] representing the base query with the base condition.
     */
    protected fun baseQuery(): SelectConditionStep<R> = dsl.selectFrom(table).where(baseCondition)

    /**
     * Combines multiple conditions using the AND operator.
     *
     * @param where Array of conditions to combine.
     * @return A single combined condition.
     */
    protected fun foldConditions(where: Array<out (T) -> Condition>) = where.fold(noCondition()) { acc, func -> acc.and(func(table)) }

    /**
     * Represents the result of an upsert operation.
     *
     * @param <R> The type of UpdatableRecord for the associated table.
     * @property record The record that was either inserted or updated.
     * @property isCreated A boolean indicating if the record was newly created (true) or updated (false).
     */
    data class UpsertResult<R : UpdatableRecord<*>>(val record: R, val isCreated: Boolean)
}
