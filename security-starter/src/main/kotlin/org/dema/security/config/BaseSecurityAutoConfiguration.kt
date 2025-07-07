package org.dema.security.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
class BaseSecurityAutoConfiguration {
    @Bean
    @Order(0)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
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
        return http.build()
    }
}