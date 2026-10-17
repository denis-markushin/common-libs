package org.dema.servicecore

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.ResourcePropertySource

/**
 * Defaults every service built on service-core starts from: actuator exposure,
 * the application tag of every meter and Logbook settings. Loaded before any
 * configuration class is parsed, so conditions see them whatever the
 * auto-configuration order, and at the lowest precedence, so every property a
 * service sets wins. The files sit next to this class, where no other jar's
 * file of the same name can shadow them.
 */
class ServiceCoreDefaultsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        listOf(
            "service-core-actuator" to "actuator.properties",
            "service-core-metrics" to "service-core.properties",
            "service-core-logbook" to "logbook.properties",
        ).forEach { (name, file) ->
            environment.propertySources.addLast(ResourcePropertySource(name, ClassPathResource(file, javaClass)))
        }
    }
}
