package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ValidationError
import org.dema.graphql.dgs.error.exception.DomainValidationException
import org.springframework.core.Ordered

class ValidationErrorMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = when (e) {
        is DomainValidationException -> ValidationError(
            message = e.message,
            path = e.path,
            value = e.value?.toString(),
        )

        else -> null
    }

    companion object {
        const val ORDER: Int = 300
    }
}
