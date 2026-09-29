package org.dema.graphql.dgs.error

import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType
import com.netflix.graphql.types.errors.TypedGraphQLError
import graphql.GraphQLError
import graphql.execution.DataFetcherExceptionHandler
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.DataFetcherExceptionHandlerResult
import java.util.concurrent.CompletableFuture

/**
 * Keeps the message of an unclassified data fetcher exception out of the
 * top-level `errors[]`. DGS's `DefaultDataFetcherExceptionHandler` writes
 * `"<class>: <message>"` of any exception that is neither a [DgsException]
 * nor Spring Security's `AccessDeniedException` into an `INTERNAL` error,
 * and a database driver message such as PostgreSQL's
 * `Detail: Key (email)=(...) already exists` carries row values. The
 * [delegate] still classifies and logs every exception; each `INTERNAL`
 * error it returns for a non-[DgsException] gets a generic message instead.
 */
class ConcealingDataFetcherExceptionHandler(
    private val delegate: DataFetcherExceptionHandler,
) : DataFetcherExceptionHandler {

    override fun handleException(
        parameters: DataFetcherExceptionHandlerParameters,
    ): CompletableFuture<DataFetcherExceptionHandlerResult> =
        delegate.handleException(parameters).thenApply { result ->
            if (parameters.exception is DgsException) {
                result
            } else {
                DataFetcherExceptionHandlerResult.newResult().errors(result.errors.map(::conceal)).build()
            }
        }

    private fun conceal(error: GraphQLError): GraphQLError =
        if (error.errorType == ErrorType.INTERNAL) {
            TypedGraphQLError.newInternalErrorBuilder()
                .message("Internal server error")
                .locations(error.locations)
                .path(error.path)
                .build()
        } else {
            error
        }
}
