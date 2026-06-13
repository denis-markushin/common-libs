package org.dema.security.config

import org.dema.security.jwt.JwtAuthoritiesConverter
import org.dema.security.jwt.SecurityJwtProperties
import org.dema.security.web.JwtAuthenticationEntryPoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.AuthenticationEntryPoint

/**
 * Registers JWT authentication beans when an OAuth2 resource server
 * [JwtDecoder] is present, i.e. when the application configures
 * `spring.security.oauth2.resourceserver.jwt.*`.
 */
@AutoConfiguration
@AutoConfigureAfter(OAuth2ResourceServerAutoConfiguration::class)
@ConditionalOnBean(JwtDecoder::class)
@EnableConfigurationProperties(SecurityJwtProperties::class)
class JwtAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun jwtAuthoritiesConverter(properties: SecurityJwtProperties): JwtAuthoritiesConverter =
        JwtAuthoritiesConverter(properties)

    @Bean
    @ConditionalOnMissingBean
    fun jwtAuthenticationEntryPoint(): AuthenticationEntryPoint = JwtAuthenticationEntryPoint()

    @Bean
    @ConditionalOnMissingBean
    fun jwtAuthCustomizer(
        converter: JwtAuthoritiesConverter,
        entryPoint: AuthenticationEntryPoint,
    ): JwtAuthCustomizer = JwtAuthCustomizer(converter, entryPoint)
}
