package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import org.dema.servicecore.properties.DemaLogbookProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration

class DemaLogbookConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DemaLogbookConfiguration::class.java))

    @Test
    fun `configuration activates and loads logbook properties when logbook is enabled by default`() {
        contextRunner.run { ctx ->
            assertThat(ctx.getBeansOfType(DemaLogbookProperties::class.java)).isNotEmpty()
            assertThat(ctx.environment.getProperty("logbook.format.style")).isEqualTo("json")
            assertThat(ctx.environment.getProperty("logbook.strategy")).isEqualTo("body-only-if-status-at-least")
        }
    }

    @Test
    fun `configuration does not activate when logbook is disabled`() {
        contextRunner
            .withPropertyValues("logbook.enabled=false")
            .run { ctx ->
                assertThat(ctx.getBeansOfType(DemaLogbookProperties::class.java)).isEmpty()
                assertThat(ctx.environment.getProperty("logbook.format.style")).isEqualTo(null)
            }
    }

    @Test
    fun `default logbook predicate include path is set to api wildcard`() {
        contextRunner.run { ctx ->
            assertThat(ctx.environment.getProperty("logbook.predicate.include[0].path")).isEqualTo("/api/**")
        }
    }

    @Test
    fun `context starts without binding errors when combined with Logbook autoconfiguration`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    DemaLogbookConfiguration::class.java,
                    LogbookAutoConfiguration::class.java,
                ),
            )
            .run { ctx ->
                assertThat(ctx.startupFailure).isNull()
            }
    }

    @Test
    fun `consumer can configure logbook predicate include path without binding errors`() {
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    DemaLogbookConfiguration::class.java,
                    LogbookAutoConfiguration::class.java,
                ),
            )
            .withPropertyValues("logbook.predicate.include[0].path=/api/**")
            .run { ctx ->
                assertThat(ctx.startupFailure).isNull()
                assertThat(ctx.environment.getProperty("logbook.predicate.include[0].path")).isEqualTo("/api/**")
            }
    }
}
