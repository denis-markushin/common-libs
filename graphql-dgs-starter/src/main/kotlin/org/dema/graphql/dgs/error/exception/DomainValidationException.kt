package org.dema.graphql.dgs.error.exception

import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType

/**
 * Thrown when an inbound request violates a domain-level validation rule (invariants, format,
 * business constraints) that goes beyond GraphQL schema validation.
 *
 * Mapped to GraphQL [ErrorType.BAD_REQUEST] and surfaced as a `ValidationError` carrying the
 * offending field [path] and rejected [value].
 */
open class DomainValidationException(
    message: String,
    val path: String? = null,
    val value: Any? = null,
    cause: Exception? = null,
) : DgsException(message = message, cause = cause, errorType = ErrorType.BAD_REQUEST)
