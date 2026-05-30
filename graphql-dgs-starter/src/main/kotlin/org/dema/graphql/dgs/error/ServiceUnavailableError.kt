package org.dema.graphql.dgs.error

data class ServiceUnavailableError(
    override val message: String,
    val retryAfterSeconds: Int? = null,
) : ErrorInterface
