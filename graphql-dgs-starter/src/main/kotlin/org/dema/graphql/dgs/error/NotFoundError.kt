package org.dema.graphql.dgs.error

data class NotFoundError(
    override val message: String,
    val entityId: String? = null,
    val entityType: String? = null,
) : ErrorInterface
