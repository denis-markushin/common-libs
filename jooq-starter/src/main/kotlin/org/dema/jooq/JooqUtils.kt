package org.dema.jooq

import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.Record
import org.jooq.Result
import org.jooq.Select
import org.jooq.SortField
import org.jooq.SortOrder
import org.jooq.Table
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
 * Offset pagination of a jOOQ select with its metadata (total rows, page size, current page, last
 * page flag) computed by window functions in the same query.
 *
 * Every overload reduces its sort to sort fields of the original select and goes through one
 * query builder; a sort field the select does not have is rejected.
 *
 * Inspired by the article: [Calculating Pagination Metadata Without Extra Roundtrips in SQL](https://blog.jooq.org/calculating-pagination-metadata-without-extra-roundtrips-in-sql/)
 */
object JooqUtils {
    private val TOTAL_ROWS: Field<Int> = count().over().`as`("total_rows")

    /**
     * Rows [offset] to [offset] + [limit] of [original] in ascending order of [sort], with the pagination metadata columns.
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        limit: Long,
        offset: Long,
    ): Result<Record> = window(ctx, original, defaults(sort), limit, offset)

    /**
     * Rows [offset] to [offset] + [limit] of [original] in the order of [sort], with the pagination metadata columns.
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<SortField<*>>,
        limit: Long,
        offset: Long,
    ): Result<Record> = window(ctx, original, sort.toList(), limit, offset)

    /**
     * Rows [offset] to [offset] + [limit] of [original] in the order of the Spring Data [sort], with the pagination metadata columns.
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Sort?,
        limit: Long,
        offset: Long,
    ): Result<Record> = window(ctx, original, spring(sort), limit, offset)

    /**
     * Page of [original] in ascending order of [sort], mapped into [targetType].
     */
    fun <T : Any> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        limit: Long,
        offset: Long,
        targetType: Class<T>,
    ): Page<T> = page(ctx, original, defaults(sort), OffsetBasedPageRequest(offset, limit)) { it.into(targetType) }

    /**
     * Page of [original] in ascending order of [sort], mapped into [targetTable].
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        limit: Long,
        offset: Long,
        targetTable: Table<T>?,
    ): Page<T> = page(ctx, original, defaults(sort), OffsetBasedPageRequest(offset, limit)) { it.into(targetTable) }

    /**
     * Page of [original] in the order of [sort], mapped into [targetTable].
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<SortField<*>>,
        limit: Long,
        offset: Long,
        targetTable: Table<T>?,
    ): Page<T> = page(ctx, original, sort.toList(), OffsetBasedPageRequest(offset, limit)) { it.into(targetTable) }

    /**
     * Page of [original] in ascending order of [sort]; an unpaged [pageable] returns every row.
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        pageable: Pageable,
    ): Page<Record> = page(ctx, original, defaults(sort), pageable) { it }

    /**
     * Page of [original] in ascending order of [sort], mapped into [targetTable]; an unpaged [pageable] returns every row.
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        pageable: Pageable,
        targetTable: Table<T>,
    ): Page<T> = page(ctx, original, defaults(sort), pageable) { it.into(targetTable) }

    /**
     * Page of [original] in the order of the Spring Data sort of [pageable]; an unpaged [pageable] returns every row.
     */
    fun paginate(
        ctx: DSLContext,
        original: Select<*>,
        pageable: Pageable,
    ): Page<Record> = page(ctx, original, spring(pageable.sort), pageable) { it }

    /**
     * Page of [original] in the order of [sort], mapped into [targetTable]; an unpaged [pageable] returns every row.
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<SortField<*>>,
        pageable: Pageable,
        targetTable: Table<T>,
    ): Page<T> = page(ctx, original, sort.toList(), pageable) { it.into(targetTable) }

    /**
     * Page of [original] in ascending order of [sort], mapped into [targetType]; an unpaged [pageable] returns every row.
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        sort: Array<Field<*>>,
        pageable: Pageable,
        targetType: Class<T>,
    ): Page<T> = page(ctx, original, defaults(sort), pageable) { it.into(targetType) }

    /**
     * Page of [original] in the order of the Spring Data sort of [pageable], mapped into [targetTable]; an unpaged
     * [pageable] returns every row.
     */
    fun <T : Record> paginate(
        ctx: DSLContext,
        original: Select<*>,
        pageable: Pageable,
        targetTable: Table<T>,
    ): Page<T> = page(ctx, original, spring(pageable.sort), pageable) { it.into(targetTable) }

    private fun <T : Any> page(
        ctx: DSLContext,
        original: Select<*>,
        sort: List<SortField<*>>,
        pageable: Pageable,
        into: (Result<Record>) -> List<T>,
    ): Page<T> {
        if (pageable.isUnpaged) return PageImpl(into(all(ctx, original, sort)))
        val rows = window(ctx, original, sort, pageable.pageSize.toLong(), pageable.offset)
        val total = rows.firstOrNull()?.let { TOTAL_ROWS[it]?.toLong() } ?: if (pageable.offset > 0) ctx.fetchCount(original).toLong() else 0L
        return PageImpl(into(rows), pageable, total)
    }

    private fun all(
        ctx: DSLContext,
        original: Select<*>,
        sort: List<SortField<*>>,
    ): Result<Record> {
        val orig = original.asTable("orig")
        return ctx.select(orig.asterisk()).from(orig).orderBy(ordered(orig, sort)).fetch()
    }

    private fun window(
        ctx: DSLContext,
        original: Select<*>,
        sort: List<SortField<*>>,
        limit: Long,
        offset: Long,
    ): Result<Record> {
        val orig = original.asTable("orig")
        val row = rowNumber().over().orderBy(ordered(orig, sort)).`as`("row_num")
        val t = ctx.select(orig.asterisk()).select(TOTAL_ROWS, row).from(orig).orderBy(ordered(orig, sort)).limit(limit).offset(offset).asTable("t")
        return ctx
            .select(*t.fields(*original.select.toTypedArray<Field<*>>()))
            .select(
                count().over().`as`("actual_page_size"),
                field(max(t.field(row)).over().eq(t.field(TOTAL_ROWS))).`as`("last_page"),
                t.field(TOTAL_ROWS),
                t.field(row),
                t.field(row)!!.minus(inline(1)).div(limit).plus(inline(1)).`as`("current_page"),
            ).from(t)
            .orderBy(ordered(t, sort))
            .fetch()
    }

    private fun ordered(table: Table<*>, sort: List<SortField<*>>): List<SortField<*>> =
        sort.map { (table.field(it.name) ?: throw IllegalArgumentException("Unknown sort property: ${it.name}")).sort(it.order) }

    private fun defaults(sort: Array<Field<*>>): List<SortField<*>> = sort.map { it.sortDefault() }

    private fun spring(sort: Sort?): List<SortField<*>> =
        sort?.map { field(name(it.property)).sort(if (it.isAscending) SortOrder.ASC else SortOrder.DESC) }?.toList().orEmpty()
}
