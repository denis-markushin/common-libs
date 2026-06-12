package org.dema.security.config

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.handler.HandlerMappingIntrospector

class SecurityStarterIntegrationTest {
    private val runner = WebApplicationContextRunner()
        .withBean(
            "mvcHandlerMappingIntrospector",
            HandlerMappingIntrospector::class.java,
            { HandlerMappingIntrospector() },
        )
        .withBean(JwtDecoder::class.java, { JwtDecoder { _ -> error("decode not used in this test") } })
        .withConfiguration(
            AutoConfigurations.of(
                BaseSecurityAutoConfiguration::class.java,
                JwtAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `base and jwt auto configs build a single filter chain`() {
        runner.run { context ->
            context.getBean(SecurityFilterChain::class.java)
            assertThat(context.getBeansOfType(SecurityFilterChain::class.java)).hasSize(1)
        }
    }

    @Test
    fun `jwt customizer is registered when decoder present`() {
        runner.run { context ->
            assertThat(context.containsBean("jwtAuthCustomizer")).isTrue()
        }
    }
}
