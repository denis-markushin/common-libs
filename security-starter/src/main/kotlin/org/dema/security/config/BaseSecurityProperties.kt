package org.dema.security.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for the shared security filter chain.
 */
@ConfigurationProperties("dema.security")
data class BaseSecurityProperties(
    /**
     * Additional ant patterns served without authentication, applied before the
     * catch-all `authenticated` rule. Use for service-specific public endpoints.
     */
    val permitAll: List<String> = emptyList(),
)
