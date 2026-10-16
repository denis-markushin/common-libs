package org.dema.servicecore

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.ResourcePropertySource

/**
 * Actuator defaults of every service: only health and prometheus over HTTP. Loaded
 * before any configuration class is parsed, so endpoint conditions such as the
 * Prometheus scrape endpoint's see the exposure whatever the auto-configuration
 * order; the lowest precedence leaves every management property of a service in charge.
 */
class ActuatorEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        environment.propertySources.addLast(ResourcePropertySource("actuator-defaults", ClassPathResource("actuator.properties")))
    }
}
