package org.dema.security.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * Basic Spring Security configuration that disables CSRF and exposes
 * a permissive default [SecurityFilterChain].
 *
 * All Swagger, actuator, GraphQL and internal endpoints are accessible
 * without authentication, while any other request requires the user to be
 * authenticated.
 */
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
class BaseSecurityAutoConfiguration {
    /**
     * Builds the default [SecurityFilterChain] applied to the application.
     *
     * @param http the HTTP security builder
     * @param customizers additional customizations applied after the base rules
     * @return configured security filter chain
     */
    @Bean
    @Order(0)
    fun defaultSecurityFilterChain(
        http: HttpSecurity,
        customizers: List<HttpSecurityCustomizer>,
    ): SecurityFilterChain {
        http {
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/actuator/**", permitAll)
                authorize("/graphql", permitAll)
                authorize("/internal/**", permitAll)
                authorize(anyRequest, authenticated)
            }
        }
        customizers.forEach { it.customize(http) }
        return http.build()
    }
}
