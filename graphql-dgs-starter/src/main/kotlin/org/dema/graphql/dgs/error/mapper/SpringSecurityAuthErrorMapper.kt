package org.dema.graphql.dgs.error.mapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ForbiddenError
import org.dema.graphql.dgs.error.UnauthorizedError
import org.springframework.core.Ordered
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException

class SpringSecurityAuthErrorMapper :
    GraphQLErrorMapper,
    Ordered {

    override fun getOrder(): Int = ORDER

    override fun map(e: Throwable): ErrorInterface? = when (e) {
        is AuthenticationException -> UnauthorizedError(message = e.message ?: "Unauthorized")
        is AccessDeniedException   -> ForbiddenError(message = e.message ?: "Forbidden")
        else -> null
    }

    companion object {
        const val ORDER: Int = 450
    }
}
