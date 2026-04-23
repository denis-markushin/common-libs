package org.dema.servicecore.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "logbook")
class DemaLogbookProperties {
    var enabled: Boolean = true
}
