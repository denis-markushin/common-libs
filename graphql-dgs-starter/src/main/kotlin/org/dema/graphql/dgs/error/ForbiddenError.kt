package org.dema.graphql.dgs.error

data class ForbiddenError(
    override val message: String,
) : ErrorInterface
