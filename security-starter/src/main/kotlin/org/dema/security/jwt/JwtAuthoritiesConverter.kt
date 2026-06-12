package org.dema.security.jwt

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * Builds an authenticated token from a JWT, extracting authorities from the
 * configured roles claim. Roles are trimmed and upper-cased.
 */
class JwtAuthoritiesConverter(
    private val properties: SecurityJwtProperties,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities: List<SimpleGrantedAuthority> = jwt.claims[properties.rolesClaim]
            .extractRoles()
            .map(String::trim)
            .map(String::uppercase)
            .map(::SimpleGrantedAuthority)
        return JwtAuthenticationToken(jwt, authorities, jwt.subject)
    }

    private fun Any?.extractRoles(): Collection<String> = when (this) {
        is String -> listOf(this)
        is Collection<*> -> mapNotNull { it?.toString() }
        is Map<*, *> -> flatMap { (_, v) -> v.extractRoles() }
        else -> emptyList()
    }
}
