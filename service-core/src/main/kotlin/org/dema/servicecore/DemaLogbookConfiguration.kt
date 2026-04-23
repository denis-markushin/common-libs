package org.dema.servicecore

import org.dema.servicecore.properties.DemaLogbookProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.PropertySource
import org.zalando.logbook.Logbook

@AutoConfiguration
@ConditionalOnClass(Logbook::class)
@ConditionalOnProperty(
    prefix = "logbook",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(DemaLogbookProperties::class)
@PropertySource("classpath:/logbook.properties")
class DemaLogbookConfiguration
