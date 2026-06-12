package org.dema.security.config

import assertk.assertThat
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
}
