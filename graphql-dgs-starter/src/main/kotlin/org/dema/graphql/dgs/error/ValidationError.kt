package org.dema.graphql.dgs.error

data class ValidationError(
    override val message: String,
    val path: String? = null,
    val value: String? = null,
) : ErrorInterface
