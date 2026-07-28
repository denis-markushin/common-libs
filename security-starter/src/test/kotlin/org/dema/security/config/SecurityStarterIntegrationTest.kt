package org.dema.security.config

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isBetween
import assertk.assertions.isTrue
import org.dema.security.filter.XRolesAuthoritiesFilter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.intercept.AuthorizationFilter
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
            context.getBean<SecurityFilterChain>()
            assertThat(context.getBeansOfType<SecurityFilterChain>()).hasSize(1)
        }
    }

    @Test
    fun `jwt customizer is registered when decoder present`() {
        runner.run { context ->
            assertThat(context.containsBean("jwtAuthCustomizer")).isTrue()
        }
    }

    @Test
    fun `x-roles filter sits in the chain before authorization`() {
        runner.withConfiguration(AutoConfigurations.of(XRolesAutoConfiguration::class.java)).run { context ->
            val filters = context.getBean<SecurityFilterChain>().filters
            val xRoles = filters.indexOfFirst { it is XRolesAuthoritiesFilter }
            val authz = filters.indexOfFirst { it is AuthorizationFilter }
            assertThat(xRoles).isBetween(0, authz - 1)
        }
    }
}
