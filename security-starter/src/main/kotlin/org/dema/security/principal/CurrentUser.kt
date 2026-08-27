package org.dema.security.principal

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

/**
 * Resolves the caller from the JWT principal held in the security context.
 *
 * The subject claim is read as a UUID, which is what the identity provider issues
 * for a user record. A request authenticated by any other means, or carrying a
 * subject that is not a UUID, counts as no caller at all rather than as a
 * half-identified one: [idOrNull] reports it absent and [id] refuses it.
 *
 * Absent credentials are an authentication failure, not an authorization one, so
 * [id] raises [AuthenticationCredentialsNotFoundException] and the caller sees 401
 * rather than 403.
 */
class CurrentUser {

    fun idOrNull(): UUID? = token()?.let { runCatching { UUID.fromString(it.token.subject) }.getOrNull() }

    fun id(): UUID {
        val token = token() ?: throw AuthenticationCredentialsNotFoundException("Request is not authenticated with a JWT")
        return runCatching { UUID.fromString(token.token.subject) }
            .getOrElse { throw AuthenticationCredentialsNotFoundException("JWT subject is not a UUID: ${token.token.subject}", it) }
    }

    private fun token(): JwtAuthenticationToken? =
        SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
}
