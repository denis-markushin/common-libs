package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.dema.graphql.dgs.error.ForbiddenError
import org.dema.graphql.dgs.error.exception.ForbiddenException
import org.dema.graphql.dgs.error.mapper.ForbiddenErrorMapper
import org.junit.jupiter.api.Test

class ForbiddenErrorMapperTest {
    private val mapper = ForbiddenErrorMapper()

    @Test
    fun `ORDER constant is 500`() {
        assertThat(ForbiddenErrorMapper.ORDER).isEqualTo(500)
        assertThat(mapper.order).isEqualTo(500)
    }

    @Test
    fun `ForbiddenException maps message`() {
        val result = mapper.map(ForbiddenException("denied"))
        assertThat(result).isEqualTo(ForbiddenError(message = "denied"))
    }

    @Test
    fun `ForbiddenException with default message`() {
        val result = mapper.map(ForbiddenException())
        assertThat(result).isEqualTo(ForbiddenError(message = "Forbidden"))
    }

    @Test
    fun `unrelated exception returns null`() {
        assertThat(mapper.map(RuntimeException("nope"))).isNull()
    }
}
