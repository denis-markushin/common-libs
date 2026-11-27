package org.demalab.servicecore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.getProperty
import org.zalando.logbook.autoconfigure.LogbookAutoConfiguration

class DemaLogbookEnvironmentPostProcessorTest {

    @Test
    fun `logbook raises its logger to trace by default`() {
        assertThat(processed().getProperty("logging.level.org.zalando.logbook"), "exchange log stays silent by default").isEqualTo("TRACE")
    }

    @Test
    fun `enabled logbook raises its logger to trace`() {
        val environment = processed("logbook.enabled" to listOf("true", "on", "yes", "1").random())
        assertThat(environment.getProperty("logging.level.org.zalando.logbook"), "enabled exchange log stays silent").isEqualTo("TRACE")
    }

    @Test
    fun `disabled logbook leaves its logger alone`() {
        assertThat(disabled().getProperty("logging.level.org.zalando.logbook"), "disabled exchange log still raised to trace").isNull()
    }

    @Test
    fun `disabled logbook switches the servlet filter off`() {
        assertThat(disabled().getProperty<Boolean>("logbook.filter.enabled"), "disabled logbook still filters every request").isEqualTo(false)
    }

    @Test
    fun `disabled logbook switches the security filter off`() {
        assertThat(disabled().getProperty<Boolean>("logbook.secure-filter.enabled"), "disabled logbook still filters every secured request").isEqualTo(false)
    }

    @Test
    fun `service filter setting wins over disabled logbook`() {
        val environment = disabled("logbook.filter.enabled" to "true")
        assertThat(environment.getProperty<Boolean>("logbook.filter.enabled"), "service-core overrode the filter setting of the service").isEqualTo(true)
    }

    @Test
    fun `disabled logbook registers no exchange filter`() {
        web().run { ctx ->
            assertThat(ctx.containsBean("logbookFilter"), "disabled logbook still filters every request").isFalse()
        }
    }

    @Test
    fun `disabled logbook registers no secure exchange filter`() {
        web().run { ctx ->
            assertThat(ctx.containsBean("secureLogbookFilter"), "disabled logbook still filters every secured request").isFalse()
        }
    }

    private fun web(): WebApplicationContextRunner =
        WebApplicationContextRunner()
            .withPropertyValues("logbook.enabled=${falsy()}")
            .withInitializer { DemaLogbookEnvironmentPostProcessor().postProcessEnvironment(it.environment, SpringApplication()) }
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration::class.java, LogbookAutoConfiguration::class.java))

    private fun disabled(vararg service: Pair<String, Any>): StandardEnvironment =
        processed("logbook.enabled" to falsy(), *service)

    private fun processed(vararg service: Pair<String, Any>): StandardEnvironment =
        StandardEnvironment().also {
            it.propertySources.addFirst(MapPropertySource("service", mapOf(*service)))
            DemaLogbookEnvironmentPostProcessor().postProcessEnvironment(it, SpringApplication())
        }

    private fun falsy(): String = listOf("false", "off", "no", "0").random()
}
