package org.dema.graphql.dgs.error.exception

import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType

/**
 * Thrown when a request lacks valid authentication credentials, or the presented credentials
 * have expired or been revoked.
 *
 * Mapped to GraphQL [ErrorType.UNAUTHENTICATED] and surfaced as an `UnauthorizedError`.
 * Use [ForbiddenException] instead when the caller is authenticated but lacks permission.
 */
open class UnauthorizedException(
    message: String = "Unauthorized",
    cause: Exception? = null,
) : DgsException(message = message, cause = cause, errorType = ErrorType.UNAUTHENTICATED)
