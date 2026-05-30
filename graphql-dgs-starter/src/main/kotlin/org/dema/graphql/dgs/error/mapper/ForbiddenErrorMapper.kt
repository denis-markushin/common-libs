package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ForbiddenError
import org.dema.graphql.dgs.error.exception.ForbiddenException
import org.springframework.core.Ordered

class ForbiddenErrorMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = when (e) {
        is ForbiddenException -> ForbiddenError(message = e.message)
        else -> null
    }

    companion object {
        const val ORDER: Int = 500
    }
}
