package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ServiceUnavailableError
import org.dema.graphql.dgs.error.exception.ServiceUnavailableException
import org.springframework.core.Ordered

class ServiceUnavailableErrorMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = when (e) {
        is ServiceUnavailableException -> ServiceUnavailableError(
            message = e.message,
            retryAfterSeconds = e.retryAfterSeconds,
        )
        else -> null
    }

    companion object {
        const val ORDER: Int = 600
    }
}
