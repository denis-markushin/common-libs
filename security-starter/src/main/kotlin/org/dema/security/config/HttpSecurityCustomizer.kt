package org.dema.security.config

import org.springframework.security.config.annotation.web.builders.HttpSecurity

/**
 * Extension point for contributing security configuration to the shared
 * [BaseSecurityAutoConfiguration] filter chain. Each authentication mechanism
 * provides one implementation as a bean.
 *
 * Implementations configure the same [HttpSecurity] instance after the base
 * rules and before the chain is built, so both the raw Java API and the Kotlin
 * `http { }` DSL are valid here.
 */
fun interface HttpSecurityCustomizer {
    fun customize(http: HttpSecurity)
}
