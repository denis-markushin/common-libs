package org.dema.security.filter

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class XRolesAuthoritiesFilterTest {
    private val filter = XRolesAuthoritiesFilter()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `adds authorities from header`() {
        val request =
            MockHttpServletRequest().apply {
                addHeader("X-Roles", "ADMIN, USER")
            }
        val response = MockHttpServletResponse()
        val chain: FilterChain = MockFilterChain()

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("user", "password")

        filter.doFilter(request, response, chain)

        val authorities = SecurityContextHolder.getContext().authentication.authorities.map { it.authority }
        assertThat(authorities).containsExactly("admin", "user")
    }

    @Test
    fun `skips when header missing`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val chain: FilterChain = MockFilterChain()

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("user", "password")

        filter.doFilter(request, response, chain)

        val authorities = SecurityContextHolder.getContext().authentication.authorities
        assertThat(authorities).isEmpty()
    }
}