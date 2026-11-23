package org.dema.graphql.dgs.pagination

import graphql.relay.ConnectionCursor
import org.dema.graphql.dgs.utils.CursorUtils.encodeToBase64
import org.jooq.Field
import org.jooq.Record

/**
 * Implementation of [ConnectionCursor] that encodes record values into Base64
 * using the provided order-by fields.
 *
 * The value is encoded on construction, so a page with a `null` sort value fails
 * inside the data fetcher with [IllegalStateException] naming the field: seek
 * pagination cannot continue past such a record.
 */
class Base64EncodedCursor<T : Record>(
    record: T,
    orderByFields: Array<out Field<*>>,
) : ConnectionCursor {
    private val token: String =
        orderByFields
            .map { field -> checkNotNull(record[field]) { "Sort field '${field.name}' must not be null in selected rows" } }
            .toTypedArray()
            .encodeToBase64()

    /** Returns the Base64 encoded cursor value. */
    override fun getValue(): String = token

    override fun toString(): String = value
}
