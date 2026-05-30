package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.dema.graphql.dgs.error.UnauthorizedError
import org.dema.graphql.dgs.error.exception.UnauthorizedException
import org.dema.graphql.dgs.error.mapper.UnauthorizedErrorMapper
import org.junit.jupiter.api.Test

class UnauthorizedErrorMapperTest {
    private val mapper = UnauthorizedErrorMapper()

    @Test
    fun `ORDER constant is 400`() {
        assertThat(UnauthorizedErrorMapper.ORDER).isEqualTo(400)
        assertThat(mapper.order).isEqualTo(400)
    }

    @Test
    fun `UnauthorizedException maps message`() {
        val result = mapper.map(UnauthorizedException("token expired"))
        assertThat(result).isEqualTo(UnauthorizedError(message = "token expired"))
    }

    @Test
    fun `UnauthorizedException with default message`() {
        val result = mapper.map(UnauthorizedException())
        assertThat(result).isEqualTo(UnauthorizedError(message = "Unauthorized"))
    }

    @Test
    fun `unrelated exception returns null`() {
        assertThat(mapper.map(RuntimeException("nope"))).isNull()
    }
}
