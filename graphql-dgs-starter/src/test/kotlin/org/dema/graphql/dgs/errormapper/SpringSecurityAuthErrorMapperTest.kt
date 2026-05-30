package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.dema.graphql.dgs.error.ForbiddenError
import org.dema.graphql.dgs.error.UnauthorizedError
import org.dema.graphql.dgs.error.mapper.SpringSecurityAuthErrorMapper
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException

class SpringSecurityAuthErrorMapperTest {
    private val mapper = SpringSecurityAuthErrorMapper()

    @Test
    fun `ORDER constant is 450`() {
        assertThat(SpringSecurityAuthErrorMapper.ORDER).isEqualTo(450)
        assertThat(mapper.order).isEqualTo(450)
    }

    @Test
    fun `AuthenticationException maps to UnauthorizedError`() {
        val result = mapper.map(BadCredentialsException("bad creds"))
        assertThat(result).isEqualTo(UnauthorizedError(message = "bad creds"))
    }

    @Test
    fun `AccessDeniedException maps to ForbiddenError`() {
        val result = mapper.map(AccessDeniedException("no permission"))
        assertThat(result).isEqualTo(ForbiddenError(message = "no permission"))
    }

    @Test
    fun `unrelated exception returns null`() {
        assertThat(mapper.map(RuntimeException("nope"))).isNull()
    }
}
