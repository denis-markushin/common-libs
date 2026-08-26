package org.dema.test.security

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.time.Instant

/**
 * Accepts any bearer token presented to a test context.
 *
 * Replaces the decoder that would otherwise fetch a JWK set and verify a signature,
 * so a secured endpoint can be exercised without an identity provider to issue
 * tokens. Import it alongside [WithMockJwt], which supplies the principal itself.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestJwtDecoderConfig {

    @Bean
    fun jwtDecoder(): JwtDecoder = JwtDecoder { token ->
        Jwt.withTokenValue(token)
            .header("alg", "none")
            .subject(SUBJECT)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(LIFESPAN))
            .build()
    }

    private companion object {
        const val SUBJECT = "00000000-0000-0000-0000-000000000000"
        const val LIFESPAN = 300L
    }
}
