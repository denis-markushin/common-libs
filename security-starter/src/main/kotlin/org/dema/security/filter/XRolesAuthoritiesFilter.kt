package org.dema.security.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

private const val AUTHORITIES_HEADER = "X-Roles"

class XRolesAuthoritiesFilter : OncePerRequestFilter() {
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
                .map(String::lowercase)
                .map(::SimpleGrantedAuthority)

        if (authorities.isEmpty()) {
            filterChain.doFilter(request, response)
            return
        }

        val auth: Authentication = SecurityContextHolder.getContext().authentication

        SecurityContextHolder.getContext().apply {
            this.authentication = UsernamePasswordAuthenticationToken(auth.principal, auth.credentials, authorities)
        }

        filterChain.doFilter(request, response)
    }
}