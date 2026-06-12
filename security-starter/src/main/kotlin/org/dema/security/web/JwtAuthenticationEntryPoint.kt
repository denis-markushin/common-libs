package org.dema.security.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

private const val GRAPHQL_UNAUTHORIZED =
    """{"errors":[{"message":"Unauthorized","extensions":{"code":"UNAUTHENTICATED"}}]}"""
private const val REST_UNAUTHORIZED = """{"message":"Unauthorized"}"""

/**
 * Renders authentication failures as JSON. GraphQL requests receive a
 * GraphQL-shaped error with HTTP 200 so the federation gateway can parse the
 * body; all other requests receive an HTTP 401 JSON payload.
 */
class JwtAuthenticationEntryPoint : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException?,
    ) {
        response.characterEncoding = Charsets.UTF_8.name()
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        if (request.requestURI.endsWith("/graphql")) {
            response.status = HttpServletResponse.SC_OK
            response.writer.write(GRAPHQL_UNAUTHORIZED)
        } else {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write(REST_UNAUTHORIZED)
        }
        response.writer.flush()
    }
}
