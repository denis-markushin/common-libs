package org.demalab.graphql.dgs.pagination

import org.demalab.graphql.dgs.error.exception.DomainValidationException
import org.demalab.graphql.dgs.utils.CursorUtils.decode
import org.demalab.graphql.dgs.utils.CursorUtils.into
import org.demalab.graphql.dgs.utils.OrderByClausesMapping
import org.jooq.SortField

/**
 * Represents Relay-style pagination parameters.
 *
 * @property sortingKey key used to resolve order-by clauses
 * @property after optional cursor of the last item fetched
 * @property first the number of items to retrieve; must be greater than zero
 */
class RelayPageable(
    val sortingKey: Enum<*>,
    val after: String?,
    val first: Int,
) {
    init {
        require(first > 0) { "`first` argument must be greater than 0" }
    }

    /** Order-by clauses associated with [sortingKey]. */
    val orderByClauses: Array<out SortField<*>> = OrderByClausesMapping.getFields(sortingKey)

    /**
     * Values decoded from [after] into the Java types of [orderByClauses] used for seek pagination.
     *
     * @throws DomainValidationException if [after] is not a cursor of [orderByClauses]
     */
    val seekValues: Array<out Any> = after?.let(::seek) ?: emptyArray()

    private fun seek(cursor: String): Array<out Any> =
        try {
            cursor.decode()
                .also { check(it.size == orderByClauses.size) { "Cursor holds ${it.size} values for ${orderByClauses.size} sort fields" } }
                .mapIndexed { index, value -> value.into(orderByClauses[index].`$field`().type) }
                .toTypedArray()
        } catch (e: RuntimeException) {
            throw DomainValidationException(message = "Invalid cursor", path = "after", value = cursor, cause = e)
        }

    override fun toString(): String = "RelayPageable(" +
        "first=$first, " +
        "after=$after, " +
        "sortingKey=$sortingKey" +
        ")"
}
