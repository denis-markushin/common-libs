package org.demalab.graphql.dgs.error.mapper

import org.demalab.graphql.dgs.error.ErrorInterface
import org.demalab.graphql.dgs.error.ForbiddenError
import org.demalab.graphql.dgs.error.exception.ForbiddenException
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
