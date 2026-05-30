package org.dema.graphql.dgs.error

data class ConflictError(
    override val message: String,
    val entityType: String? = null,
    val entityId: String? = null,
    val reason: String? = null,
) : ErrorInterface
