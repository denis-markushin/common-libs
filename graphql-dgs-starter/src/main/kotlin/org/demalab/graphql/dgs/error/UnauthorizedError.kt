package org.demalab.graphql.dgs.error

data class UnauthorizedError(
    override val message: String,
) : ErrorInterface
