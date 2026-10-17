package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import org.dema.servicecore.properties.DemaLogbookProperties
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.zalando.logbook.Strategy
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration
import org.zalando.logbook.core.WithoutBodyStrategy

class DemaLogbookConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(DemaLogbookConfiguration::class.java))

    @Test
    fun `configuration activates when logbook is enabled by default`() {
        contextRunner.run { ctx ->
            assertThat(ctx.getBeansOfType(DemaLogbookProperties::class.java), "logbook settings never reached the context").isNotEmpty()
        }
    }

    @Test
    fun `configuration does not activate when logbook is disabled`() {
        contextRunner
            .withPropertyValues("logbook.enabled=false")
            .run { ctx ->
                assertThat(ctx.getBeansOfType(DemaLogbookProperties::class.java), "disabled logbook still configured").isEmpty()
            }
    }

    @Test
    fun `context starts without binding errors when combined with Logbook autoconfiguration`() {
        ApplicationContextRunner()
            .withInitializer(defaults())
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    DemaLogbookConfiguration::class.java,
                    LogbookAutoConfiguration::class.java,
                ),
            )
            .run { ctx ->
                assertThat(ctx.startupFailure, "logbook settings broke the context").isNull()
            }
    }

    @Test
    fun `default strategy logs exchanges without bodies`() {
        ApplicationContextRunner()
            .withInitializer(defaults())
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    DemaLogbookConfiguration::class.java,
                    LogbookAutoConfiguration::class.java,
                ),
            )
            .run { ctx ->
                val strategy = ctx.getBean(Strategy::class.java)
                assertThat(strategy, "exchange log writes bodies by default").isInstanceOf(WithoutBodyStrategy::class)
            }
    }

    @Test
    fun `consumer can configure logbook predicate include path without binding errors`() {
        ApplicationContextRunner()
            .withInitializer(defaults())
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    DemaLogbookConfiguration::class.java,
                    LogbookAutoConfiguration::class.java,
                ),
            )
            .withPropertyValues("logbook.predicate.include[0].path=/api/**")
            .run { ctx ->
                assertThat(ctx.startupFailure, "consumer path setting broke logbook binding").isNull()
            }
    }

    private fun defaults(): ApplicationContextInitializer<ConfigurableApplicationContext> =
        ApplicationContextInitializer {
            ServiceCoreDefaultsEnvironmentPostProcessor().postProcessEnvironment(it.environment, SpringApplication())
        }
}
