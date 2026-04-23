package org.dema.servicecore

import org.springframework.boot.actuate.autoconfigure.availability.AvailabilityHealthContributorAutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.PropertySource

@AutoConfiguration(before = [AvailabilityHealthContributorAutoConfiguration::class])
@PropertySource("classpath:/actuator.properties")
class ActuatorAutoConfiguration
