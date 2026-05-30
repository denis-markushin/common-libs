package org.dema.graphql.dgs.error.exception

import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType

/**
 * Thrown when a downstream dependency or the service itself is temporarily unable to handle the
 * request (overload, maintenance, transient outage, circuit breaker open).
 *
 * Mapped to GraphQL [ErrorType.UNAVAILABLE] and surfaced as a `ServiceUnavailableError`.
 * The optional [retryAfterSeconds] hints the client when to retry.
 */
open class ServiceUnavailableException(
    message: String = "Service unavailable",
    val retryAfterSeconds: Int? = null,
    cause: Exception? = null,
) : DgsException(message = message, cause = cause, errorType = ErrorType.UNAVAILABLE)
