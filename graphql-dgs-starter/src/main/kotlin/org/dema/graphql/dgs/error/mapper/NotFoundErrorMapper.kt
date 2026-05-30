package org.dema.graphql.dgs.error.mapper

import com.netflix.graphql.dgs.exceptions.DgsEntityNotFoundException
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.NotFoundError
import org.dema.graphql.dgs.error.exception.EntityNotFoundException
import org.springframework.core.Ordered

class NotFoundErrorMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = when (e) {
        is EntityNotFoundException -> NotFoundError(
            message = e.message,
            entityId = e.entityId?.toString(),
            entityType = e.entityType,
        )
        is DgsEntityNotFoundException -> NotFoundError(message = e.message)
        else -> null
    }

    companion object {
        const val ORDER: Int = 100
    }
}
