package org.dema.jooq

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Result
import org.jooq.Select
import org.jooq.SortField
import org.jooq.SortOrder
import org.jooq.Table
import org.jooq.TableField
import org.jooq.impl.DSL.asterisk
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.inline
import org.jooq.impl.DSL.max
import org.jooq.impl.DSL.name
import org.jooq.impl.DSL.rowNumber
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * Utility object containing helper functions for jOOQ-based operations.
 * This class provides pagination, sorting, and query utilities that are used to work with DSLContext.
 *
 * Inspired by the article: [Calculating Pagination Metadata Without Extra Roundtrips in SQL](https://blog.jooq.org/calculating-pagination-metadata-without-extra-roundtrips-in-sql/)
 */
object JooqUtils {
    private const val ORIGINAL_SUBQUERY_NAME: String = "orig"
    private const val TOTAL_ROWS_COLUMN_NAME: String = "total_rows"
    private const val ROW_NUMBER_COLUMN_NAME: String = "row_num"
    private const val ACTUAL_PAGE_SIZE_COLUMN_NAME: String = "actual_page_size"
    private const val LAST_PAGE_FLAG_COLUMN_NAME: String = "last_page"
    private const val CURRENT_PAGE_COLUMN_NAME: String = "current_page"
    private val TOTAL_ROWS_FIELD: Field<Int> = count().over().`as`(TOTAL_ROWS_COLUMN_NAME)

    /**
     * Paginates the given select query with the specified sorting fields, limit, and offset.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of fields to sort by.
     * @param limit The maximum number of records to be returned.
     * @param offset The offset from where to start returning records.
     * @return The paginated result as a [Result] of [Record].
     */
    private fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        limit: Long,
        offset: Long,
    ): Result<Record> {
        val orig = original.asTable(ORIGINAL_SUBQUERY_NAME)
        val row = rowNumber().over().orderBy(*orig.fields(*sort)).`as`(ROW_NUMBER_COLUMN_NAME)

        val t: Table<*> =
            ctx
                .select(orig.asterisk())
                .select(TOTAL_ROWS_FIELD, row)
                .from(orig)
                .orderBy(*orig.fields(*sort))
                .limit(limit)
                .offset(offset)
                .asTable("t")

        return ctx
            .select(*t.fields(*original.select.toTypedArray<Field<*>>()))
            .select(
                count().over().`as`(ACTUAL_PAGE_SIZE_COLUMN_NAME),
                field(max(t.field(row)).over().eq(t.field(TOTAL_ROWS_FIELD))).`as`(LAST_PAGE_FLAG_COLUMN_NAME),
                t.field(TOTAL_ROWS_FIELD),
                t.field(row),
                t.field(row)!!.minus(inline(1)).div(limit).plus(inline(1)).`as`(CURRENT_PAGE_COLUMN_NAME),
            ).from(t)
            .orderBy(*t.fields(*sort)).fetch()
    }

    /**
     * Paginates the given select query with the specified sorting fields, limit, and offset.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of sorting fields ([SortField]) that defines the sorting order.
     * @param limit The maximum number of records to return.
     * @param offset The offset from where to start returning records.
     * @return The paginated result as a [Result] of [Record].
     *
     * This method performs pagination on the provided query by adding sorting fields, a limit, and an offset.
     * It creates an intermediate table representation to efficiently compute pagination metadata, including
     * the total row count and current page number.
     * The query is executed against the given [DSLContext].
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<SortField<*>>,
        limit: Long,
        offset: Long,
    ): Result<Record> {
        val orig = original.asTable(ORIGINAL_SUBQUERY_NAME)
        val sortOrderMap = sort.associateBy({ it.name }, { it.order })

        val fields = sort.map { field(name(it.name)) }.toTypedArray()
        val row = rowNumber().over().orderBy(*orig.fields(*fields)).`as`(ROW_NUMBER_COLUMN_NAME)

        val t: Table<*> =
            ctx
                .select(orig.asterisk())
                .select(TOTAL_ROWS_FIELD, row)
                .from(orig)
                .orderBy(orig.fields(*fields).map { if (sortOrderMap[it!!.name] == SortOrder.DESC) it.desc() else it.asc() })
                .limit(limit)
                .offset(offset)
                .asTable("t")

        return ctx
            .select(*t.fields(*original.select.toTypedArray()))
            .select(
                count().over().`as`(ACTUAL_PAGE_SIZE_COLUMN_NAME),
                field(max(t.field(row)).over().eq(t.field(TOTAL_ROWS_FIELD))).`as`(LAST_PAGE_FLAG_COLUMN_NAME),
                t.field(TOTAL_ROWS_FIELD),
                t.field(row),
                t.field(row)!!.minus(inline(1)).div(limit).plus(inline(1)).`as`(CURRENT_PAGE_COLUMN_NAME),
            ).from(t)
            .orderBy(t.fields(*fields).map { if (sortOrderMap[it!!.name] == SortOrder.DESC) it.desc() else it.asc() })
            .fetch()
    }

    /**
     * Paginates the given select query with the specified sorting fields, limit, offset, and target type.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of fields that defines the sorting order.
     * @param limit The maximum number of records to return.
     * @param offset The offset from where to start returning records.
     * @param targetType The target type class to map the results into.
     * @return A [Page] of the target records of type [T], representing the paginated result.
     *
     * This method performs pagination on the provided query by adding sorting fields, a limit, and an offset.
     * It creates an intermediate table representation to efficiently compute pagination metadata and maps the result
     * into the specified target type [T].
     * The pagination metadata includes the total row count and current page.
     */
    fun <T> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        limit: Long,
        offset: Long,
        targetType: Class<T>,
    ): Page<T> {
        val res = paginate(ctx, original, sort, limit, offset)

        val pageable = OffsetBasedPageRequest(offset, limit)
        if (res.isEmpty()) return Page.empty(pageable)

        val anyRec = res.first()
        val totalRows = getTotalRows(anyRec)

        return PageImpl(res.into(targetType), pageable, totalRows.toLong())
    }

    /**
     * Paginates the given select query with the specified sorting fields, limit, offset, and target table.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of fields that defines the sorting order.
     * @param limit The maximum number of records to return.
     * @param offset The offset from where to start returning records.
     * @param targetTable The target table into which the results should be mapped.
     * @return A [Page] of the target records of type [T], representing the paginated result.
     *
     * This method performs pagination on the provided query by adding sorting fields, a limit, and an offset.
     * It uses an intermediate table representation to efficiently compute pagination metadata and maps the results
     * into the specified target table [T].
     * The pagination metadata includes the total row count and current page.
     */
    fun <T : Record?> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        limit: Long,
        offset: Long,
        targetTable: Table<T>?,
    ): Page<T> {
        val res = paginate(ctx, original, sort, limit, offset)

        val pageable = OffsetBasedPageRequest(offset, limit)
        if (res.isEmpty()) return Page.empty(pageable)

        val anyRec = res.stream().findAny().get()
        val totalRows = getTotalRows(anyRec)

        return PageImpl(res.into(targetTable), pageable, totalRows.toLong())
    }

    /**
     * Paginates the given select query with the specified sorting fields, limit, offset, and target table.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of [SortField] that defines the sorting order.
     * @param limit The maximum number of records to return.
     * @param offset The offset from where to start returning records.
     * @param targetTable The target table into which the results should be mapped.
     * @return A [Page] of the target records of type [T], representing the paginated result.
     *
     * This method performs pagination on the provided query by adding sorting fields, a limit, and an offset.
     * It uses an intermediate table representation to efficiently compute pagination metadata and maps the results
     * into the specified target table [T].
     * The pagination metadata includes the total row count and current page.
     */
    fun <T : Record?> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<SortField<*>>,
        limit: Long,
        offset: Long,
        targetTable: Table<T>?,
    ): Page<T> {
        val res = paginate(ctx, original, sort, limit, offset)

        val pageable = OffsetBasedPageRequest(offset, limit)
        if (res.isEmpty()) return Page.empty(pageable)

        val anyRec = res.first()
        val totalRows = getTotalRows(anyRec)

        return PageImpl(res.into(targetTable), pageable, totalRows.toLong())
    }

    /**
     * Paginates the given select query with the specified sorting fields and pageable information.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of fields that defines the sorting order.
     * @param pageable The pagination information, including page size and offset.
     * @return A [Page] of [Record], representing the paginated result.
     *
     * This method performs pagination based on the provided pageable information and sorts the results according to the specified fields.
     * It handles both paginated and unpaged requests, providing the appropriate result set.
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        pageable: Pageable,
    ): Page<Record> {
        if (pageable.isUnpaged) {
            return PageImpl(
                ctx.select(asterisk())
                    .from(original)
                    .orderBy(*original.fields(*sort))
                    .fetch(),
            )
        }

        val res = paginate(ctx, original, sort, pageable.pageSize.toLong(), pageable.offset)
        if (res.isEmpty()) return Page.empty(pageable)

        val anyRec = res.stream().findAny().get()
        val totalRows = getTotalRows(anyRec)

        return PageImpl(res, pageable, totalRows.toLong())
    }

    /**
     * Paginates the given select query with the specified sorting fields, pageable information, and target table.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of fields that defines the sorting order.
     * @param pageable The pagination information, including page size and offset.
     * @param targetTable The target table into which the results should be mapped.
     * @return A [Page] of the target records of type [T], representing the paginated result.
     *
     * This method performs pagination on the provided query by adding sorting fields and pageable information.
     * The results are mapped into the specified target table [T], and pagination metadata includes the total row count.
     */
    fun <T : Record?> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        pageable: Pageable,
        targetTable: Table<T>,
    ): Page<T> {
        if (pageable.isUnpaged) {
            return PageImpl(
                ctx.select(asterisk())
                    .from(original)
                    .orderBy(*original.fields(*sort))
                    .fetchInto(targetTable),
            )
        }

        return paginate(ctx, original, sort, pageable.pageSize.toLong(), pageable.offset, targetTable)
    }

    /**
     * Paginates the given select query with pageable information.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param pageable The pagination information, including page size and offset.
     * @return A [Page] of [Record], representing the paginated result.
     *
     * This method handles both paginated and unpaged requests.
     * If pageable is unpaged, it returns all records from the query.
     * Otherwise, it applies pagination and returns the appropriate result set.
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        pageable: Pageable,
    ): Page<Record> {
        if (pageable.isUnpaged) {
            return PageImpl(ctx.select(asterisk()).from(original).fetch())
        }

        val res = paginate(ctx, original, arrayOf<Field<*>>(), pageable.pageSize.toLong(), pageable.offset)
        if (res.isEmpty()) return Page.empty(pageable)

        val anyRec = res.first()
        val totalRows = getTotalRows(anyRec)

        return PageImpl(res, pageable, totalRows.toLong())
    }

    /**
     * Paginates the given select query with the specified sorting fields, pageable information, and target table.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of [SortField] that defines the sorting order.
     * @param pageable The pagination information, including page size and offset.
     * @param targetTable The target table into which the results should be mapped.
     * @return A [Page] of the target records of type [T], representing the paginated result.
     *
     * This method performs pagination on the provided query by adding sorting fields and pageable information.
     * The results are mapped into the specified target table [T].
     * It also handles sorting directions to provide
     * the correct order of results.
     */
    fun <T : Record?> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<SortField<*>>,
        pageable: Pageable,
        targetTable: Table<T>,
    ): Page<T> {
        val sortOrderMap = sort.associateBy({ it.name }, { it.order })
        val fields = sort.map { field(name(it.name)) }.toTypedArray()

        if (pageable.isUnpaged) {
            return PageImpl(
                ctx.select(asterisk())
                    .from(original)
                    .orderBy(original.fields(*fields).map { if (sortOrderMap[it!!.name] == SortOrder.DESC) it.desc() else it.asc() })
                    .fetchInto(targetTable),
            )
        }

        return paginate(ctx, original, sort, pageable.pageSize.toLong(), pageable.offset, targetTable)
    }

    /**
     * Paginates the given select query with the specified sorting fields, pageable information, and target type.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of fields that defines the sorting order.
     * @param pageable The pagination information, including page size and offset.
     * @param targetType The target type class to map the results into.
     * @return A [Page] of the target records of type [T], representing the paginated result.
     *
     * This method performs pagination on the provided query by adding sorting fields and pageable information.
     * If the pageable is unpaged, it returns all the results sorted by the given fields.
     * Otherwise, it applies pagination to the query and maps the results into the specified target type [T].
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        pageable: Pageable,
        targetType: Class<T>,
    ): Page<T> {
        if (pageable.isUnpaged) {
            return PageImpl(
                ctx.select(asterisk())
                    .from(original)
                    .orderBy(*original.fields(*sort))
                    .fetchInto(targetType),
            )
        }

        return paginate(ctx, original, sort, pageable.pageSize.toLong(), pageable.offset, targetType)
    }

    /**
     * Paginates the given select query based on the provided pageable information and target table.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param pageable The pagination information, including page size and offset.
     * @param targetTable The target table that the results should be mapped to.
     * @return A [Page] of the target records, representing the paginated result.
     *
     * This method supports both paginated and unpaged requests.
     * If the pageable is unpaged, it returns all records.
     * Otherwise, it uses the provided pagination information (limit and offset) to return a subset of records.
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        pageable: Pageable,
        targetTable: Table<T>,
    ): Page<T> {
        if (pageable.isUnpaged) {
            return PageImpl(
                ctx.select(asterisk())
                    .from(original)
                    .fetchInto(targetTable),
            )
        }

        val limit = pageable.pageSize.toLong()
        val offset = pageable.offset
        val res = paginate(ctx, original, pageable.sort, limit, offset)

        if (res.isEmpty()) return Page.empty(pageable)

        val anyRec = res.stream().findAny().get()
        val totalRows = getTotalRows(anyRec)

        return PageImpl(res.into(targetTable), pageable, totalRows.toLong())
    }

    /**
     * Paginates the given select query with the specified sorting fields, limit, and offset.
     *
     * @param ctx The DSLContext to execute the query.
     * @param original The original select query that should be paginated.
     * @param sort An array of fields to sort by.
     * @param limit The maximum number of records to be returned.
     * @param offset The offset from where to start returning records.
     * @return The paginated result as a [Result] of [Record].
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Sort?,
        limit: Long,
        offset: Long,
    ): Result<Record> {
        val orig = original.asTable(ORIGINAL_SUBQUERY_NAME)
        val sortOrig = getSortFields(sort, orig)

        val row = rowNumber().over().orderBy(sortOrig).`as`(ROW_NUMBER_COLUMN_NAME)

        val t: Table<*> =
            ctx
                .select(orig.asterisk())
                .select(TOTAL_ROWS_FIELD, row)
                .from(orig)
                .orderBy(sortOrig)
                .limit(limit)
                .offset(offset)
                .asTable("t")

        return ctx
            .select(*t.fields(*original.select.toTypedArray<Field<*>>()))
            .select(
                count().over().`as`(ACTUAL_PAGE_SIZE_COLUMN_NAME),
                field(max(t.field(row)).over().eq(t.field(TOTAL_ROWS_FIELD))).`as`(
                    LAST_PAGE_FLAG_COLUMN_NAME,
                ),
                t.field(TOTAL_ROWS_FIELD),
                t.field(row),
                t.field(row)!!.minus(inline(1)).div(limit).plus(inline(1))
                    .`as`(CURRENT_PAGE_COLUMN_NAME),
            ).from(t)
            .orderBy(getSortFields(sort, t))
            .fetch()
    }

    /**
     * Retrieves the total number of rows from the provided record.
     *
     * @param rec The record containing the total rows field.
     * @return The total number of rows, or 0 if not available.
     */
    private fun getTotalRows(rec: Record): Int {
        return TOTAL_ROWS_FIELD[rec] ?: 0
    }

    /**
     * Converts a Spring Data [Sort] object into a list of jOOQ [SortField] objects for the given table.
     *
     * @param sort The Spring Data Sort object containing sorting information.
     * @param table The table for which to generate sort fields.
     * @return A list of [SortField] objects representing the sorting logic.
     */
    private fun getSortFields(
        sort: Sort?,
        table: Table<*>,
    ): List<SortField<*>> {
        if (sort == null) {
            return listOf()
        }

        return sort.map {
            val sortFieldName = it.property
            val sortDirection = it.direction

            val tableField = getTableField(sortFieldName, table)
            convertTableFieldToSortField(tableField, sortDirection)
        }.toList()
    }

    /**
     * Retrieves the corresponding table field for the given sort field name.
     *
     * @param sortFieldName The name of the field to sort by.
     * @param table The table containing the field.
     * @return The [TableField] that corresponds to the sort field name.
     */
    private fun getTableField(
        sortFieldName: String,
        table: Table<*>,
    ): TableField<*, *> {
        return table.field(sortFieldName) as TableField<*, *>
    }

    /**
     * Converts a given [TableField] into a [SortField] with the specified sort direction.
     *
     * @param tableField The table field to be converted.
     * @param sortDirection The direction of sorting, either ASC or DESC.
     * @return The [SortField] for the provided table field with the specified direction.
     */
    private fun convertTableFieldToSortField(
        tableField: TableField<*, *>,
        sortDirection: Sort.Direction,
    ): SortField<*> {
        return if (sortDirection == Sort.Direction.ASC) tableField.asc() else tableField.desc()
    }
}