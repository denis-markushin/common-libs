package org.dema.graphql.dgs.error

data class UnauthorizedError(
    override val message: String,
) : ErrorInterface
