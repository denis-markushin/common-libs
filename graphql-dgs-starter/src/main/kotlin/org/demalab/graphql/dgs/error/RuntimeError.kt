package org.demalab.graphql.dgs.error

data class RuntimeError(
    override val message: String,
) : ErrorInterface
