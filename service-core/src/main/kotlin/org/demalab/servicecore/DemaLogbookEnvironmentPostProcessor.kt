package org.demalab.servicecore

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.getProperty

/**
 * Owner of the `logbook.enabled` switch. Enabled, it raises the Logbook
 * logger to TRACE, the level Logbook writes exchanges at. Disabled, it
 * switches off both Logbook servlet filters, so no request pays for
 * exchange capture. Added at the lowest precedence, so any Logbook
 * property a service sets wins.
 */
class DemaLogbookEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        environment.propertySources.addLast(
            if (environment.getProperty<Boolean>("logbook.enabled", true)) {
                MapPropertySource("logbook-logging", mapOf("logging.level.org.zalando.logbook" to "TRACE"))
            } else {
                MapPropertySource(
                    "logbook-off",
                    mapOf("logbook.filter.enabled" to "false", "logbook.secure-filter.enabled" to "false"),
                )
            },
        )
    }
}
