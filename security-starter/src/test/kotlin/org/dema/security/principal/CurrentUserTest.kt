package org.dema.security.principal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.messageContains
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Instant
import java.util.UUID

class CurrentUserTest {

    @Test
    fun `reports no id when the context holds nothing`() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext())
        assertThat(
            CurrentUser().idOrNull(),
            "an unauthenticated request must not resolve to a user",
        ).isNull()
    }

    @Test
    fun `reports no id when the request was authenticated without a JWT`() {
        authenticate(UsernamePasswordAuthenticationToken("dispatcher", "n/a", emptyList()))
        assertThat(
            CurrentUser().idOrNull(),
            "a non-JWT principal carries no subject claim and must not be mistaken for a user",
        ).isNull()
    }

    @Test
    fun `reports no id when the subject is not a UUID`() {
        authenticate(jwt("svc-account-7"))
        assertThat(
            CurrentUser().idOrNull(),
            "a subject that is not a UUID must not be reported as a user id",
        ).isNull()
    }

    @Test
    fun `reads the subject claim as the user id`() {
        authenticate(jwt("3f0b71ae-9c24-4d18-bb53-2e6017c9a840"))
        assertThat(
            CurrentUser().idOrNull(),
            "the id must come from the subject claim of the presented token",
        ).isEqualTo(UUID.fromString("3f0b71ae-9c24-4d18-bb53-2e6017c9a840"))
    }

    @Test
    fun `refuses to name a user when the request is unauthenticated`() {
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext())
        assertThrows<AuthenticationCredentialsNotFoundException>(
            "an unauthenticated request must fail as missing credentials, not silently yield a user",
        ) { CurrentUser().id() }
    }

    @Test
    fun `names the offending subject when it is not a UUID`() {
        authenticate(jwt("svc-account-7"))
        val thrown = assertThrows<AuthenticationCredentialsNotFoundException> { CurrentUser().id() }
        assertThat(
            thrown,
            "the failure must say which subject was rejected, otherwise it cannot be diagnosed",
        ).messageContains("svc-account-7")
    }

    private fun jwt(subject: String): JwtAuthenticationToken = JwtAuthenticationToken(
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(subject)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .build(),
    )

    private fun authenticate(token: org.springframework.security.core.Authentication) {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = token
        SecurityContextHolder.setContext(context)
    }
}
