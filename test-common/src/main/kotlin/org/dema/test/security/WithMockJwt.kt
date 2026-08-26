package org.dema.test.security

import org.springframework.security.test.context.support.WithSecurityContext

/**
 * Authenticates a test with a stub JWT principal.
 *
 * Stands in for the resource server exchange, so a test can name a subject and a
 * set of authorities without an identity provider to obtain a real token from.
 * Authorities are taken verbatim: pass whatever string the authorization rules
 * under test compare against.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@WithSecurityContext(factory = WithMockJwtSecurityContextFactory::class)
annotation class WithMockJwt(
    val userId: String = "00000000-0000-0000-0000-000000000001",
    val roles: Array<String> = [],
)
