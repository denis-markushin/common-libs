package org.dema.servicecore

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.health.autoconfigure.application.AvailabilityHealthContributorAutoConfiguration
import org.springframework.context.annotation.PropertySource

@AutoConfiguration(before = [AvailabilityHealthContributorAutoConfiguration::class])
@PropertySource("classpath:/actuator.properties")
class ActuatorAutoConfiguration
