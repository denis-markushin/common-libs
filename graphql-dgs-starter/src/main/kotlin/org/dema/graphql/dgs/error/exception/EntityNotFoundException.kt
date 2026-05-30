package org.dema.graphql.dgs.error.exception

import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType

/**
 * Thrown when a requested domain entity cannot be located by its identifier or query criteria.
 *
 * Mapped to GraphQL [ErrorType.NOT_FOUND] and surfaced as a `NotFoundError` in the response payload.
 */
open class EntityNotFoundException(
    val entityType: String,
    val entityId: Any? = null,
    message: String? = null,
    cause: Exception? = null,
) : DgsException(
    message = message ?: "$entityType not found${entityId?.let { ": $it" } ?: ""}",
    cause = cause,
    errorType = ErrorType.NOT_FOUND,
)
