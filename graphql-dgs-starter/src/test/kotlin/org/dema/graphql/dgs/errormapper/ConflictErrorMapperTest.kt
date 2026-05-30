package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.dema.graphql.dgs.error.ConflictError
import org.dema.graphql.dgs.error.exception.ConflictException
import org.dema.graphql.dgs.error.mapper.ConflictErrorMapper
import org.junit.jupiter.api.Test

class ConflictErrorMapperTest {
    private val mapper = ConflictErrorMapper()

    @Test
    fun `ORDER constant is 200`() {
        assertThat(ConflictErrorMapper.ORDER).isEqualTo(200)
        assertThat(mapper.order).isEqualTo(200)
    }

    @Test
    fun `ConflictException maps fields to ConflictError`() {
        val e = ConflictException(
            message = "duplicate",
            reason = "ALREADY_ASSIGNED",
            entityType = "Project",
            entityId = 42,
        )
        val result = mapper.map(e)
        assertThat(result).isEqualTo(
            ConflictError(
                message = "duplicate",
                entityType = "Project",
                entityId = "42",
                reason = "ALREADY_ASSIGNED",
            ),
        )
    }

    @Test
    fun `ConflictException without optional fields maps with nulls`() {
        val e = ConflictException(message = "simple")
        val result = mapper.map(e)
        assertThat(result).isEqualTo(
            ConflictError(message = "simple", entityType = null, entityId = null, reason = null),
        )
    }

    @Test
    fun `unrelated exception returns null`() {
        assertThat(mapper.map(RuntimeException("nope"))).isNull()
    }
}
