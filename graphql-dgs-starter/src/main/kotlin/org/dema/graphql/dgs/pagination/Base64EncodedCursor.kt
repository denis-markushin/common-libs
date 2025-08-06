package org.dema.graphql.dgs.pagination

import graphql.relay.ConnectionCursor
import org.dema.graphql.dgs.utils.CursorUtils.encodeToBase64
import org.jooq.Field
import org.jooq.Record

/**
 * Implementation of [ConnectionCursor] that encodes record values into Base64
 * using the provided order-by fields.
 */
class Base64EncodedCursor<T : Record>(
    private val record: T,
    private val orderByFields: Array<out Field<*>>,
) : ConnectionCursor {
    /** Returns the Base64 encoded cursor value. */
    override fun getValue(): String = record.into(*orderByFields).intoArray().encodeToBase64()

    override fun toString(): String = value
}
