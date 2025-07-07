package org.dema.security.config

import org.dema.security.filter.XRolesAuthoritiesFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@AutoConfiguration
@Profile("!prod & !production")
class XRolesAutoConfiguration {
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    fun headerAuthoritiesFilterRegistration(): FilterRegistrationBean<XRolesAuthoritiesFilter> =
        FilterRegistrationBean(XRolesAuthoritiesFilter())
}