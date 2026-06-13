package org.dema.security.config

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
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
}
