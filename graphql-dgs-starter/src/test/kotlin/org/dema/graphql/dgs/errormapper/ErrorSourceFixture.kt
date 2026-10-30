package org.dema.graphql.dgs.errormapper

import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ErrorSource
import org.dema.graphql.dgs.error.exception.ConflictException

/**
 * Error of a made-up booking domain, the one a test exception hands to the client.
 */
internal data class SeatTakenError(
    override val message: String,
) : ErrorInterface

/**
 * Plain exception that carries the error it answers with.
 */
internal class SeatTakenException(
    override val error: ErrorInterface,
) : RuntimeException("seat taken"),
    ErrorSource

/**
 * Subclass of a built-in exception that carries its own error, so the built-in mapper claims it too.
 */
internal class SeatTakenConflictException(
    override val error: ErrorInterface,
) : ConflictException(message = "seat taken"),
    ErrorSource
