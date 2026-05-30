package org.dema.graphql.dgs.error.exception

import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType

/**
 * Thrown when a request cannot be completed because the current entity state conflicts with the
 * requested operation (duplicate keys, stale versions, illegal state transitions).
 *
 * Mapped to GraphQL [ErrorType.FAILED_PRECONDITION] and surfaced as a `ConflictError` with
 * optional [reason], [entityType], and [entityId] for client-side reconciliation.
 */
open class ConflictException(
    message: String,
    val reason: String? = null,
    val entityType: String? = null,
    val entityId: Any? = null,
    cause: Exception? = null,
) : DgsException(message = message, cause = cause, errorType = ErrorType.FAILED_PRECONDITION)
