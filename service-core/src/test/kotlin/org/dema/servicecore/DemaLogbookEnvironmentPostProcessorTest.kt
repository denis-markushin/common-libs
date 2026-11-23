package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
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

    private val processor = DemaLogbookEnvironmentPostProcessor()
    private val application = SpringApplication()

    @Test
    fun `adds logbook logging trace property when logbook is enabled by default`() {
        val environment = StandardEnvironment()

        processor.postProcessEnvironment(environment, application)

        val source = environment.propertySources["logbook-logging"]
        assertThat(source).isNotNull()
        assertThat(source!!.getProperty("logging.level.org.zalando.logbook")).isEqualTo("TRACE")
    }

    @Test
    fun `adds logbook logging trace property when logbook is explicitly enabled`() {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource("test", mapOf("logbook.enabled" to "true")),
        )

        processor.postProcessEnvironment(environment, application)

        val source = environment.propertySources["logbook-logging"]
        assertThat(source).isNotNull()
        assertThat(source!!.getProperty("logging.level.org.zalando.logbook")).isEqualTo("TRACE")
    }

    @Test
    fun `does not add logbook logging property when logbook is disabled`() {
        val environment = StandardEnvironment()
        environment.propertySources.addFirst(
            MapPropertySource("test", mapOf("logbook.enabled" to "false")),
        )

        processor.postProcessEnvironment(environment, application)

        val source = environment.propertySources["logbook-logging"]
        assertThat(source).isNull()
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
        WebApplicationContextRunner()
            .withPropertyValues("logbook.enabled=false")
            .withInitializer { DemaLogbookEnvironmentPostProcessor().postProcessEnvironment(it.environment, SpringApplication()) }
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration::class.java, LogbookAutoConfiguration::class.java))
            .run { ctx ->
                assertThat(ctx.containsBean("logbookFilter"), "disabled logbook still filters every request").isFalse()
            }
    }

    private fun disabled(vararg service: Pair<String, Any>): StandardEnvironment =
        StandardEnvironment().also {
            it.propertySources.addFirst(MapPropertySource("service", mapOf("logbook.enabled" to "false", *service)))
            DemaLogbookEnvironmentPostProcessor().postProcessEnvironment(it, SpringApplication())
        }
}
