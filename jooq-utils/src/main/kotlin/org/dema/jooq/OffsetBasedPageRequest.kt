package org.dema.jooq

import java.io.Serializable
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class OffsetBasedPageRequest(offset: Long, limit: Long, sort: Sort = Sort.unsorted()) :
    Pageable, Serializable {
    private val limit: Long
    private val offset: Long
    private val sort: Sort

    /**
     * Creates a new [OffsetBasedPageRequest] with sort parameters applied.
     *
     * @param offset     zero-based offset.
     * @param limit      the size of the elements to be returned.
     * @param direction  the direction of the [Sort] to be specified, can be null.
     * @param properties the properties to sort by, must not be null or empty.
     */
    constructor(offset: Int, limit: Int, direction: Sort.Direction, vararg properties: String) : this(
        offset.toLong(),
        limit.toLong(),
        Sort.by(direction, *properties),
    )

    init {
        require(offset >= 0) { "Offset index must not be less than zero!" }
        require(limit >= 1) { "Limit must not be less than one!" }
        this.limit = limit
        this.offset = offset
        this.sort = sort
    }

    override fun getPageNumber(): Int {
        return (offset / limit).toInt()
    }

    override fun getPageSize(): Int {
        return limit.toInt()
    }

    override fun getOffset(): Long {
        return offset
    }

    override fun getSort(): Sort {
        return sort
    }

    override fun next(): Pageable {
        return OffsetBasedPageRequest((offset + pageSize).toInt().toLong(), pageSize.toLong(), sort)
    }

    private fun previous(): OffsetBasedPageRequest {
        return if (hasPrevious()) {
            OffsetBasedPageRequest(
                (offset - pageSize).toInt().toLong(),
                pageSize.toLong(),
                sort,
            )
        } else {
            this
        }
    }

    override fun previousOrFirst(): Pageable {
        return if (hasPrevious()) previous() else first()
    }

    override fun first(): Pageable {
        return OffsetBasedPageRequest(0, pageSize.toLong(), sort)
    }

    override fun withPage(pageNumber: Int): Pageable {
        TODO("Not yet implemented")
    }

    override fun hasPrevious(): Boolean {
        return offset > 0
    }
}