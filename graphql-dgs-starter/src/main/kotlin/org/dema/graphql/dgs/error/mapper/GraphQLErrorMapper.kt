package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface

/**
 * Maps a [Throwable] to a typed [ErrorInterface], or returns `null` to delegate
 * to the next mapper in the chain.
 *
 * Implementations are discovered as Spring beans and ordered via
 * [org.springframework.core.Ordered] (preferred — exposes the order as a
 * named constant) or `@Order` (legacy). Place domain-specific mappers
 * relative to the default mappers' `companion ORDER` constants, e.g.
 * `NotFoundErrorMapper.ORDER + 50`.
 */
fun interface GraphQLErrorMapper {
    fun map(e: Throwable): ErrorInterface?
}
