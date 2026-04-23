package org.dema.servicecore

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class DemaLogbookEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication
    ) {
        val logbookEnabled = environment.getProperty("logbook.enabled", Boolean::class.java, true)
        if (!logbookEnabled) return
        environment.propertySources.addLast(
            MapPropertySource(
                "logbook-logging",
                mapOf("logging.level.org.zalando.logbook" to "TRACE"),
            ),
        )
    }
}
