package org.dema.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/** Header name that may contain a comma separated list of authorities. */
private const val AUTHORITIES_HEADER = "X-Roles"

/**
 * A servlet filter that extracts authorities from the `X-Roles` header and
 * appends them to the current [Authentication].
 */
class XRolesAuthoritiesFilter : OncePerRequestFilter() {
    /**
     * Adds authorities from the `X-Roles` header to the current authentication
     * if the header is present and contains values.
     */
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader(AUTHORITIES_HEADER)
        if (header.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }
        val authorities: List<SimpleGrantedAuthority> =
            header
                .split(",")
                .map(String::trim)
                .filter(String::isNotBlank)
                .map(String::uppercase)
                .map(::SimpleGrantedAuthority)

        if (authorities.isEmpty()) {
            filterChain.doFilter(request, response)
            return
        }

        val existing: Authentication? = SecurityContextHolder.getContext().authentication
        val updated = if (existing == null) {
            UsernamePasswordAuthenticationToken("x-roles-user", null, authorities)
        } else {
            UsernamePasswordAuthenticationToken(existing.principal, existing.credentials, authorities)
        }
        SecurityContextHolder.getContext().authentication = updated

        filterChain.doFilter(request, response)
    }
}
