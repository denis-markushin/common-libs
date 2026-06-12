package org.dema.security.web

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class JwtAuthenticationEntryPointTest {
    private val entryPoint = JwtAuthenticationEntryPoint()

    @Test
    fun `graphql request body carries UNAUTHENTICATED code`() {
        val request = MockHttpServletRequest().apply { requestURI = "/graphql" }
        val response = MockHttpServletResponse()
        entryPoint.commence(request, response, null)
        assertThat(response.contentAsString).contains("UNAUTHENTICATED")
    }

    @Test
    fun `graphql request stays status 200`() {
        val request = MockHttpServletRequest().apply { requestURI = "/graphql" }
        val response = MockHttpServletResponse()
        entryPoint.commence(request, response, null)
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun `rest request returns status 401`() {
        val request = MockHttpServletRequest().apply { requestURI = "/api/things" }
        val response = MockHttpServletResponse()
        entryPoint.commence(request, response, null)
        assertThat(response.status).isEqualTo(401)
    }

    @Test
    fun `response declares json content type`() {
        val request = MockHttpServletRequest().apply { requestURI = "/graphql" }
        val response = MockHttpServletResponse()
        JwtAuthenticationEntryPoint().commence(request, response, null)
        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE)).isNotNull().contains("application/json")
    }

    @Test
    fun `path merely ending in graphql is treated as rest`() {
        val request = MockHttpServletRequest().apply { requestURI = "/api/mygraphql" }
        val response = MockHttpServletResponse()
        JwtAuthenticationEntryPoint().commence(request, response, null)
        assertThat(response.status).isEqualTo(401)
    }
}
