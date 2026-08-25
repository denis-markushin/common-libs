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
 * A servlet filter that turns the `X-Roles` header into the request's
 * [Authentication] authorities: it replaces the authorities of an existing
 * authentication, falling back to the principal `x-roles-user` whenever the
 * security context is empty or its authentication carries no principal.
 */
class XRolesAuthoritiesFilter : OncePerRequestFilter() {
    /**
     * Replaces the current authentication's authorities with those parsed from
     * the `X-Roles` header, using the `x-roles-user` principal when no
     * authentication or no principal is present; leaves the context untouched
     * when the header yields no values.
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
            UsernamePasswordAuthenticationToken(existing.principal ?: "x-roles-user", existing.credentials, authorities)
        }
        SecurityContextHolder.getContext().authentication = updated

        filterChain.doFilter(request, response)
    }
}
