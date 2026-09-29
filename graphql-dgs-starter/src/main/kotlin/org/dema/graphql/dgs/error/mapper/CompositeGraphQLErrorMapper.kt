package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.RuntimeError

/**
 * Walks the ordered chain of [GraphQLErrorMapper]s and returns the first
 * non-null mapping. If every mapper returns `null`, falls back to a
 * [RuntimeError] with a generic message: an unclassified exception may carry
 * internals such as database row values, so its message and type stay on the
 * server.
 */
class CompositeGraphQLErrorMapper(
    private val mappers: List<GraphQLErrorMapper>,
) {
    fun toGraphQLError(e: Throwable): ErrorInterface =
        mappers.firstNotNullOfOrNull { it.map(e) }
            ?: RuntimeError(message = "Internal server error")
}
