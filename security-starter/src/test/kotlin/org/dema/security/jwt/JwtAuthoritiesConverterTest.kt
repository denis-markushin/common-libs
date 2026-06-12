package org.dema.security.jwt

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class JwtAuthoritiesConverterTest {
    private val converter = JwtAuthoritiesConverter(SecurityJwtProperties())

    private fun jwt(claims: Map<String, Any>): Jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-1")
            .claims { it.putAll(claims) }
            .build()

    @Test
    fun `extracts and uppercases roles from realm_access`() {
        val token = jwt(mapOf("realm_access" to mapOf("roles" to listOf("admin", "user"))))
        val authorities = converter.convert(token).authorities.map { it.authority }
        assertThat(authorities).containsExactlyInAnyOrder("ADMIN", "USER")
    }

    @Test
    fun `reads roles from configured claim name`() {
        val custom = JwtAuthoritiesConverter(SecurityJwtProperties(rolesClaim = "groups"))
        val token = jwt(mapOf("groups" to listOf("editor")))
        val authorities = custom.convert(token).authorities.map { it.authority }
        assertThat(authorities).containsExactlyInAnyOrder("EDITOR")
    }

    @Test
    fun `returns no authorities when claim absent`() {
        val token = jwt(mapOf("other" to "x"))
        val authorities = converter.convert(token).authorities
        assertThat(authorities).isEmpty()
    }

    @Test
    fun `extracts single string role`() {
        val token = jwt(mapOf("realm_access" to "admin"))
        val authorities = converter.convert(token).authorities.map { it.authority }
        assertThat(authorities).containsExactlyInAnyOrder("ADMIN")
    }
}
