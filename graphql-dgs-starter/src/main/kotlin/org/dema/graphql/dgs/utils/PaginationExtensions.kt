package org.dema.graphql.dgs.utils

import graphql.relay.Connection
import graphql.relay.DefaultConnection
import graphql.relay.DefaultEdge
import graphql.relay.DefaultPageInfo
import graphql.relay.Edge
import graphql.relay.PageInfo
import org.dema.graphql.dgs.pagination.Base64EncodedCursor
import org.dema.graphql.dgs.pagination.RelayPageable
import org.dema.jooq.AbstractRepository
import org.jooq.Condition
import org.jooq.Record
import org.jooq.Result
import org.jooq.Table
import org.jooq.UpdatableRecord

/**
 * Fetches records using Relay pagination parameters.
 *
 * @param relayPageable pagination configuration
 * @param where additional jOOQ condition
 * @return the resulting [Result] containing at most `first + 1` records
 */
fun <T : Table<R>, R : UpdatableRecord<*>> AbstractRepository<T, R>.getAllBy(
    relayPageable: RelayPageable,
    where: Condition,
): Result<R> {
    return getAllBy(
        orderBy = relayPageable.orderByClauses,
        seekValues = relayPageable.seekValues,
        limit = relayPageable.first + 1, // fetch one extra to detect hasNextPage in RecordsConnection
    ) { where }
}

/**
 * Converts this list of records into a GraphQL [Connection] according to
 * the provided [RelayPageable].
 */
fun <T : Record> List<T>.toConnection(pageable: RelayPageable): Connection<T> {
    if (isEmpty()) return emptyConnection()

    val fields = pageable.orderByClauses.map { it.`$field`() }.toTypedArray()
    val edges: MutableList<Edge<T>> =
        this.map {
            DefaultEdge(it, Base64EncodedCursor(it, fields))
        }.toMutableList()

    val hasPrevPage = !pageable.after.isNullOrEmpty()
    val hasNextPage = edges.size > pageable.first
    if (hasNextPage) edges.removeLast()

    val pageInfo =
        DefaultPageInfo(
            edges.first().cursor,
            edges.last().cursor,
            hasPrevPage,
            hasNextPage,
        )
    return DefaultConnection(edges, pageInfo)
}

/** Creates an empty [Connection] with no edges and default page info. */
private fun <T> emptyConnection(): Connection<T> {
    val pageInfo: PageInfo = DefaultPageInfo(null, null, false, false)
    return DefaultConnection(emptyList(), pageInfo)
}