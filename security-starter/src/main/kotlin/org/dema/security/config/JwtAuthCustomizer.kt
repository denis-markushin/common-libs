package org.dema.security.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.web.AuthenticationEntryPoint

/**
 * Enables the OAuth2 resource server on the shared filter chain, plugging in
 * the role-extracting converter and the JSON authentication entry point.
 */
class JwtAuthCustomizer(
    private val converter: Converter<Jwt, AbstractAuthenticationToken>,
    private val entryPoint: AuthenticationEntryPoint,
) : HttpSecurityCustomizer {
    override fun customize(http: HttpSecurity) {
        http {
            oauth2ResourceServer {
                authenticationEntryPoint = entryPoint
                jwt {
                    jwtAuthenticationConverter = converter
                }
            }
        }
    }
}
