package org.dema.test.security

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.test.context.support.WithSecurityContextFactory
import java.time.Instant

/**
 * Builds the security context behind [WithMockJwt].
 *
 * Produces the same [JwtAuthenticationToken] shape a resource server creates from
 * a validated bearer token, so authorization rules see a principal indistinguishable
 * from a production one.
 */
class WithMockJwtSecurityContextFactory : WithSecurityContextFactory<WithMockJwt> {

    override fun createSecurityContext(annotation: WithMockJwt): SecurityContext {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(annotation.userId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFESPAN))
            .build()
        val authorities = annotation.roles.map(::SimpleGrantedAuthority)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = JwtAuthenticationToken(jwt, authorities, annotation.userId)
        return context
    }

    private companion object {
        const val LIFESPAN = 300L
    }
}
