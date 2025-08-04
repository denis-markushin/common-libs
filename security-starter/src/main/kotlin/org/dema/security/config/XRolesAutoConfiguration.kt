package org.dema.security.config

import org.dema.security.filter.XRolesAuthoritiesFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

/**
 * Registers a filter that extracts user authorities from the `X-Roles` header.
 *
 * This configuration is enabled only for non-production profiles where
 * authenticating via custom headers is acceptable.
 */
@AutoConfiguration
@Profile("!prod & !production")
class XRolesAutoConfiguration {
    /**
     * Adds [XRolesAuthoritiesFilter] with the lowest precedence so that it
     * executes after other security filters.
     */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun headerAuthoritiesFilterRegistration(): FilterRegistrationBean<XRolesAuthoritiesFilter> =
        FilterRegistrationBean(XRolesAuthoritiesFilter())
}