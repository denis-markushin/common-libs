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
import org.jooq.impl.DSL.noCondition
import org.springframework.core.convert.ConversionService

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
): Result<R> = getAllBy(
    orderBy = relayPageable.orderByClauses,
    seekValues = relayPageable.seekValues,
    limit = relayPageable.first + 1, // fetch one extra to detect hasNextPage in RecordsConnection
) { where }

/**
 * Fetches a Relay page of records and converts it into GraphQL type [V].
 *
 * The [filter] is turned into a jOOQ [Condition] by [cs]; without a filter only the base condition of the
 * repository applies. Each record of the page is converted into [V] by the same [cs].
 *
 * @param cs conversion service that knows the filter-to-condition and record-to-[V] converters
 * @param pageable pagination configuration
 * @param filter GraphQL filter input, or `null` for none
 * @return the page as a [Connection] of [V]
 */
inline fun <reified V : Any, T : Table<R>, R : UpdatableRecord<*>> AbstractRepository<T, R>.page(
    cs: ConversionService,
    pageable: RelayPageable,
    filter: Any? = null,
): Connection<V> = getAllBy(relayPageable = pageable, where = cs.convert<Condition>(filter) ?: noCondition())
    .toConnection(pageable)
    .map { cs.convert<V>(it)!! }

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

inline fun <T, R> Connection<T>.map(transform: (T) -> R): DefaultConnection<R> {
    val edges = this.edges.map {
        val newNode = transform(it.node)
        DefaultEdge(newNode, it.cursor)
    }
    return DefaultConnection(edges, this.pageInfo)
}

/** Creates an empty [Connection] with no edges and default page info. */
private fun <T> emptyConnection(): Connection<T> {
    val pageInfo: PageInfo = DefaultPageInfo(null, null, false, false)
    return DefaultConnection(emptyList(), pageInfo)
}

/**
 * Converts the given [src] object to the target type [T] using this [ConversionService].
 *
 * This function is an inline reified wrapper around [ConversionService.convert], allowing
 * the target type to be inferred without explicitly passing a `Class<T>` instance.
 *
 * @param src the source object to convert, or `null`
 * @return the converted object of type [T], or `null` if the source is `null`
 *
 * @throws org.springframework.core.convert.ConversionFailedException if the conversion fails
 * @see ConversionService.convert
 */
inline fun <reified T : Any> ConversionService.convert(src: Any?): T? = src?.let { this.convert(src, T::class.java) }
