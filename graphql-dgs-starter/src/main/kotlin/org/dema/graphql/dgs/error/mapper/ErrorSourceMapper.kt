package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ErrorSource
import org.springframework.core.Ordered

/**
 * Answers with the error an exception carries. Runs ahead of every built-in mapper, so an exception that is
 * both an [ErrorSource] and a subclass of a built-in one, such as
 * [org.dema.graphql.dgs.error.exception.ConflictException], keeps its own error instead of the built-in one.
 */
class ErrorSourceMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = (e as? ErrorSource)?.error

    companion object {
        const val ORDER: Int = 0
    }
}
