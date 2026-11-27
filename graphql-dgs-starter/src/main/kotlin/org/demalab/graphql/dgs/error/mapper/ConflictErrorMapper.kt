package org.demalab.graphql.dgs.error.mapper

import org.demalab.graphql.dgs.error.ConflictError
import org.demalab.graphql.dgs.error.ErrorInterface
import org.demalab.graphql.dgs.error.exception.ConflictException
import org.springframework.core.Ordered

class ConflictErrorMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = when (e) {
        is ConflictException -> ConflictError(
            message = e.message,
            entityType = e.entityType,
            entityId = e.entityId?.toString(),
            reason = e.reason,
        )
        else -> null
    }

    companion object {
        const val ORDER: Int = 200
    }
}
