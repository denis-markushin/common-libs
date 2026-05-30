package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.netflix.graphql.dgs.exceptions.DgsEntityNotFoundException
import org.dema.graphql.dgs.error.NotFoundError
import org.dema.graphql.dgs.error.exception.EntityNotFoundException
import org.dema.graphql.dgs.error.mapper.NotFoundErrorMapper
import org.junit.jupiter.api.Test
import java.util.UUID

class NotFoundErrorMapperTest {
    private val mapper = NotFoundErrorMapper()

    @Test
    fun `ORDER constant is 100`() {
        assertThat(NotFoundErrorMapper.ORDER).isEqualTo(100)
        assertThat(mapper.order).isEqualTo(100)
    }

    @Test
    fun `EntityNotFoundException maps with entityId, entityType and message`() {
        val id = UUID.randomUUID()
        val e = EntityNotFoundException(entityType = "Project", entityId = id)
        val result = mapper.map(e)
        assertThat(result).isEqualTo(
            NotFoundError(message = "Project not found: $id", entityId = id.toString(), entityType = "Project"),
        )
    }

    @Test
    fun `bare DgsEntityNotFoundException maps with message only`() {
        val e = DgsEntityNotFoundException("legacy missing")
        val result = mapper.map(e)
        assertThat(result).isEqualTo(NotFoundError(message = "legacy missing"))
    }

    @Test
    fun `unrelated exception returns null`() {
        assertThat(mapper.map(IllegalStateException("nope"))).isNull()
    }
}
