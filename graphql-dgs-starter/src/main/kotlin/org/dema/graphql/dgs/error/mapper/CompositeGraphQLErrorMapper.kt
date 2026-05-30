package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.RuntimeError

/**
 * Walks the ordered chain of [GraphQLErrorMapper]s and returns the first
 * non-null mapping. If every mapper returns `null`, falls back to
 * [RuntimeError] populated from the exception's message or class name.
 */
class CompositeGraphQLErrorMapper(
    private val mappers: List<GraphQLErrorMapper>,
) {
    fun toGraphQLError(e: Throwable): ErrorInterface =
        mappers.firstNotNullOfOrNull { it.map(e) }
            ?: RuntimeError(message = e.message ?: e::class.simpleName ?: "Unknown error")
}
