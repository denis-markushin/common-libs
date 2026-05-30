package org.dema.graphql.dgs.mutation

import graphql.schema.DataFetchingEnvironment
import io.github.oshai.kotlinlogging.KotlinLogging
import org.dema.graphql.dgs.error.mapper.CompositeGraphQLErrorMapper

private val log = KotlinLogging.logger {}

internal class DefaultMutationResolver(
    private val errorMapper: CompositeGraphQLErrorMapper,
) : MutationResolver {

    override fun <T> DataFetchingEnvironment.resolveMutation(block: () -> T): MutationOutcome<T> =
        try {
            MutationOutcome.Success(block())
        } catch (e: Exception) {
            // java.util.concurrent.CancellationException is the JDK base; kotlinx CancellationException extends it.
            if (e is java.util.concurrent.CancellationException) throw e
            log.warn(e) { "Mutation failed: ${field.name}" }
            if (selectionSet.contains("error")) {
                MutationOutcome.Failure(errorMapper.toGraphQLError(e))
            } else {
                throw e
            }
        }
}
