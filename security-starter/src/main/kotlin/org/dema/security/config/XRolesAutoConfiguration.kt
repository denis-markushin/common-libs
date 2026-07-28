package org.dema.security.config

import org.dema.security.filter.XRolesAuthoritiesFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.web.access.intercept.AuthorizationFilter

/**
 * Contributes the X-Roles header mechanism to the shared security filter chain.
 *
 * Non-production only: authenticating via a plain header is a development
 * convenience and must never reach prod profiles.
 */
@AutoConfiguration
@Profile("!prod & !production")
class XRolesAutoConfiguration {
    /**
     * Inserts [XRolesAuthoritiesFilter] before authorization so header-provided
     * authorities are visible to URL rules and method security alike.
     */
    @Bean
    fun xRolesCustomizer(): HttpSecurityCustomizer = HttpSecurityCustomizer { http ->
        http.addFilterBefore(XRolesAuthoritiesFilter(), AuthorizationFilter::class.java)
    }
}
