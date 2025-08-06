package org.dema.graphql.dgs.pagination

import org.dema.graphql.dgs.utils.CursorUtils.decode
import org.dema.graphql.dgs.utils.OrderByClausesMapping
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

    /** Values decoded from [after] used for seek pagination. */
    val seekValues: Array<String> =
        after?.decode()?.also { values ->
            check(values.size == orderByClauses.size) {
                "Cursor size: [${values.size}] does not match orderBy fields size: [${orderByClauses.size}]"
            }
        } ?: emptyArray()
}
