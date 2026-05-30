package org.dema.graphql.dgs.error.exception

import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType

/**
 * Thrown when an authenticated caller is not permitted to perform the requested operation
 * (authorization failure, insufficient role or scope).
 *
 * Mapped to GraphQL [ErrorType.PERMISSION_DENIED] and surfaced as a `ForbiddenError`.
 * Use [UnauthorizedException] instead when no valid credentials are present.
 */
open class ForbiddenException(
    message: String = "Forbidden",
    cause: Exception? = null,
) : DgsException(message = message, cause = cause, errorType = ErrorType.PERMISSION_DENIED)
