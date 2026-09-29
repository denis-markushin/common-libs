@file:Suppress("DEPRECATION")

package org.dema.security.config

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isNotEmpty
import org.dema.security.filter.XRolesAuthoritiesFilter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.handler.HandlerMappingIntrospector

class XRolesAutoConfigurationTest {
    private val runner = WebApplicationContextRunner()
        .withBean(
            "mvcHandlerMappingIntrospector",
            HandlerMappingIntrospector::class.java,
            { HandlerMappingIntrospector() },
        )
        .withConfiguration(
            AutoConfigurations.of(
                BaseSecurityAutoConfiguration::class.java,
                XRolesAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `x-roles filter stays out of the chain when no profile is active`() {
        runner.run { context ->
            assertThat(context.getBean<SecurityFilterChain>().filters.filterIsInstance<XRolesAuthoritiesFilter>())
                .isEmpty()
        }
    }

    @Test
    fun `x-roles filter stays out of the chain under an unlisted profile`() {
        runner.withPropertyValues("spring.profiles.active=dev").run { context ->
            assertThat(context.getBean<SecurityFilterChain>().filters.filterIsInstance<XRolesAuthoritiesFilter>())
                .isEmpty()
        }
    }

    @Test
    fun `x-roles filter joins the chain under the local profile`() {
        runner.withPropertyValues("spring.profiles.active=local").run { context ->
            assertThat(context.getBean<SecurityFilterChain>().filters.filterIsInstance<XRolesAuthoritiesFilter>())
                .isNotEmpty()
        }
    }

    @Test
    fun `x-roles filter joins the chain under the integration-test profile`() {
        runner.withPropertyValues("spring.profiles.active=integration-test").run { context ->
            assertThat(context.getBean<SecurityFilterChain>().filters.filterIsInstance<XRolesAuthoritiesFilter>())
                .isNotEmpty()
        }
    }

    @Test
    fun `x-roles filter joins the chain when enabled by property`() {
        runner.withPropertyValues("dema.security.x-roles.enabled=true").run { context ->
            assertThat(context.getBean<SecurityFilterChain>().filters.filterIsInstance<XRolesAuthoritiesFilter>())
                .isNotEmpty()
        }
    }

    @Test
    fun `x-roles filter stays out of the chain under prod even when enabled by property`() {
        runner.withPropertyValues("spring.profiles.active=prod", "dema.security.x-roles.enabled=true").run { context ->
            assertThat(context.getBean<SecurityFilterChain>().filters.filterIsInstance<XRolesAuthoritiesFilter>())
                .isEmpty()
        }
    }
}
