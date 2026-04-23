package org.dema.servicecore

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.PropertySource

@AutoConfiguration
@PropertySource("classpath:/service-core.properties")
class ServiceCorePropertiesAutoConfiguration
