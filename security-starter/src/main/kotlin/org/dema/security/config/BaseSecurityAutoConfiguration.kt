package org.dema.security.config

import org.dema.security.principal.CurrentUser
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
@EnableConfigurationProperties(BaseSecurityProperties::class)
class BaseSecurityAutoConfiguration {
    /**
     * Builds the default [SecurityFilterChain] applied to the application.
     *
     * @param http the HTTP security builder
     * @param customizers additional customizations applied after the base rules
     * @return configured security filter chain
     */
    /**
     * Exposes the caller behind the current request.
     *
     * Registered here rather than beside the JWT beans so that it is available to
     * any application on this starter, including one whose authentication is
     * contributed by something other than a resource server.
     */
    @Bean
    @ConditionalOnMissingBean
    fun currentUser(): CurrentUser = CurrentUser()

    @Bean
    @Order(0)
    fun defaultSecurityFilterChain(
        http: HttpSecurity,
        customizers: List<HttpSecurityCustomizer>,
        properties: BaseSecurityProperties,
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
                properties.permitAll.forEach { authorize(it, permitAll) }
                authorize(anyRequest, authenticated)
            }
        }
        customizers.forEach { it.customize(http) }
        return http.build()
    }
}
