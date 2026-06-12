package org.dema.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for JWT-based authority extraction.
 */
@ConfigurationProperties("dema.security.jwt")
data class SecurityJwtProperties(
    /**
     * JWT claim name that holds user roles. Every string leaf found under the
     * claim becomes an authority, so a nested `realm_access` -> { roles: [...] }
     * resolves to its role list.
     */
    val rolesClaim: String = "realm_access",
)
