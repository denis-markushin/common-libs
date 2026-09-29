@file:Suppress("DEPRECATION")

package org.dema.security.config

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.web.FilterChainProxy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.handler.HandlerMappingIntrospector
import java.util.concurrent.atomic.AtomicBoolean

class BaseSecurityAutoConfigurationTest {
    private val runner = WebApplicationContextRunner()
        .withBean(
            "mvcHandlerMappingIntrospector",
            HandlerMappingIntrospector::class.java,
            { HandlerMappingIntrospector() },
        )
        .withConfiguration(AutoConfigurations.of(BaseSecurityAutoConfiguration::class.java))

    @Test
    fun `applies registered http security customizers`() {
        val invoked = AtomicBoolean(false)
        runner.withBean(HttpSecurityCustomizer::class.java, { HttpSecurityCustomizer { invoked.set(true) } })
            .run { context ->
                context.getBean(SecurityFilterChain::class.java)
                assertThat(invoked.get()).isTrue()
            }
    }

    @Test
    fun `binds extra permit-all patterns`() {
        runner
            .withPropertyValues(
                "dema.security.permit-all[0]=/api/v1/provider/**",
                "dema.security.permit-all[1]=/dev/sign/**",
            )
            .run { context ->
                assertThat(context.getBean(BaseSecurityProperties::class.java).permitAll).hasSize(2)
            }
    }

    @Test
    fun `builds chain with extra permit-all patterns`() {
        runner
            .withPropertyValues("dema.security.permit-all[0]=/dev/sign/**")
            .run { context ->
                assertThat(context.getBean(SecurityFilterChain::class.java)).isNotNull()
            }
    }

    @Test
    fun `anonymous heapdump request never reaches the application`() {
        runner.run { context ->
            val chain = MockFilterChain()
            FilterChainProxy(context.getBean(SecurityFilterChain::class.java))
                .doFilter(MockHttpServletRequest("GET", "/actuator/heapdump"), MockHttpServletResponse(), chain)
            assertThat(chain.request, "anonymous caller downloads a heap dump full of secrets").isNull()
        }
    }

    @Test
    fun `anonymous health probe reaches the application`() {
        runner.run { context ->
            val chain = MockFilterChain()
            FilterChainProxy(context.getBean(SecurityFilterChain::class.java))
                .doFilter(MockHttpServletRequest("GET", "/actuator/health"), MockHttpServletResponse(), chain)
            assertThat(chain.request, "unauthenticated health probe is turned away").isNotNull()
        }
    }

    @Test
    fun `anonymous prometheus scrape reaches the application`() {
        runner.run { context ->
            val chain = MockFilterChain()
            FilterChainProxy(context.getBean(SecurityFilterChain::class.java))
                .doFilter(MockHttpServletRequest("GET", "/actuator/prometheus"), MockHttpServletResponse(), chain)
            assertThat(chain.request, "unauthenticated metrics scraper is turned away").isNotNull()
        }
    }
}
