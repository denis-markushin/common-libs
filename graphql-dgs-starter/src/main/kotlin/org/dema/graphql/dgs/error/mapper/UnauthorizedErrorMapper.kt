package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.UnauthorizedError
import org.dema.graphql.dgs.error.exception.UnauthorizedException
import org.springframework.core.Ordered

class UnauthorizedErrorMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = when (e) {
        is UnauthorizedException -> UnauthorizedError(message = e.message)
        else -> null
    }

    companion object {
        const val ORDER: Int = 400
    }
}
