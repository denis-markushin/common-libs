package org.dema.security.config

import org.dema.security.filter.XRolesAuthoritiesFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase
import org.springframework.context.annotation.Profile
import org.springframework.security.web.access.intercept.AuthorizationFilter

/**
 * Contributes the X-Roles header mechanism to the shared security filter chain.
 *
 * Opt-in only: authenticating via a plain header is a development convenience,
 * so it stays off unless the `local` or `integration-test` profile is active or
 * `dema.security.x-roles.enabled` is `true`, and never reaches prod profiles.
 */
@AutoConfiguration
@Profile("!prod & !production")
@Conditional(XRolesAutoConfiguration.OptIn::class)
class XRolesAutoConfiguration {
    /**
     * Inserts [XRolesAuthoritiesFilter] before authorization so header-provided
     * authorities are visible to URL rules and method security alike.
     */
    @Bean
    fun xRolesCustomizer(): HttpSecurityCustomizer = HttpSecurityCustomizer { http ->
        http.addFilterBefore(XRolesAuthoritiesFilter(), AuthorizationFilter::class.java)
    }

    /**
     * Matches when a development profile is active or the header mechanism is
     * enabled explicitly.
     */
    class OptIn : AnyNestedCondition(ConfigurationPhase.PARSE_CONFIGURATION) {
        @Profile("local", "integration-test")
        class Profiled

        @ConditionalOnBooleanProperty("dema.security.x-roles.enabled")
        class Enabled
    }
}
