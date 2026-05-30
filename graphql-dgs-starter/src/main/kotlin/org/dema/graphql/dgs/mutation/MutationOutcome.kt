package org.dema.graphql.dgs.mutation

import org.dema.graphql.dgs.error.ErrorInterface

/**
 * Result of a mutation resolution.
 *
 * - [Success] wraps the value produced by the mutation block.
 * - [Failure] wraps a typed [ErrorInterface] when the client selected the
 *   `error` field on the mutation payload. When the client did NOT select
 *   `error`, the exception is rethrown instead of being wrapped — the
 *   conversion happens in [MutationResolver.resolveMutation].
 *
 * See the GraphQL mutation error convention (rule 6.6.4) referenced in
 * the spec.
 */
sealed interface MutationOutcome<out T> {
    data class Success<T>(
        val value: T,
    ) : MutationOutcome<T>
    data class Failure(
        val error: ErrorInterface,
    ) : MutationOutcome<Nothing>
}
