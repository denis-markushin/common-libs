package org.dema.graphql.dgs.mutation

import com.netflix.graphql.dgs.exceptions.DgsException
import graphql.schema.DataFetchingEnvironment
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.Level
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
            if (selectionSet.contains("error")) {
                log.at(level(e)) {
                    message = "Mutation failed: ${field.name}"
                    cause = e
                }
                MutationOutcome.Failure(errorMapper.toGraphQLError(e))
            } else {
                throw e
            }
        }

    private fun level(e: Exception): Level =
        Level.valueOf(((e as? DgsException)?.logLevel ?: org.slf4j.event.Level.ERROR).name)
}
