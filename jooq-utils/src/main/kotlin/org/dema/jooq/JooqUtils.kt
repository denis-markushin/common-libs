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

object JooqUtils {
    private const val ORIGINAL_SUBQUERY_NAME: String = "orig"
    private const val TOTAL_ROWS_COLUMN_NAME: String = "total_rows"
    private const val ROW_NUMBER_COLUMN_NAME: String = "row_num"
    private const val ACTUAL_PAGE_SIZE_COLUMN_NAME: String = "actual_page_size"
    private const val LAST_PAGE_FLAG_COLUMN_NAME: String = "last_page"
    private const val CURRENT_PAGE_COLUMN_NAME: String = "current_page"
    private val TOTAL_ROWS_FIELD: Field<Int> = count().over().`as`(TOTAL_ROWS_COLUMN_NAME)

    fun paginate(
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

    private fun getTotalRows(rec: Record): Int {
        return TOTAL_ROWS_FIELD[rec] ?: 0
    }

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

    private fun getTableField(
        sortFieldName: String,
        table: Table<*>,
    ): TableField<*, *> {
        return table.field(sortFieldName) as TableField<*, *>
    }

    private fun convertTableFieldToSortField(
        tableField: TableField<*, *>,
        sortDirection: Sort.Direction,
    ): SortField<*> {
        return if (sortDirection == Sort.Direction.ASC) tableField.asc() else tableField.desc()
    }
}