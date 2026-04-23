package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

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
            MapPropertySource("test", mapOf("logbook.enabled" to "true"))
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
            MapPropertySource("test", mapOf("logbook.enabled" to "false"))
        )

        processor.postProcessEnvironment(environment, application)

        val source = environment.propertySources["logbook-logging"]
        assertThat(source).isNull()
    }
}
