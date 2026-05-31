package org.dema.jooq.autoconfigure

import org.dema.jooq.timestamps.TimestampsProperties
import org.dema.jooq.timestamps.TimestampsRecordListener
import org.jooq.DSLContext
import org.jooq.RecordListenerProvider
import org.jooq.impl.DefaultRecordListenerProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import java.time.Clock

/**
 * Autoconfiguration for the jooq-starter module. Wires the timestamps RecordListener and a
 * default Clock bean when no overrides are provided by the application.
 *
 * @author Denis Markushin
 */
@AutoConfiguration
@ConditionalOnClass(DSLContext::class)
@EnableConfigurationProperties(TimestampsProperties::class)
class JooqStarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun jooqClock(): Clock = Clock.systemDefaultZone()

    @Bean
    @ConditionalOnProperty(
        prefix = "dema.jooq.timestamps",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun timestampsRecordListenerProvider(
        clock: Clock,
        properties: TimestampsProperties,
    ): RecordListenerProvider = DefaultRecordListenerProvider(
        TimestampsRecordListener(
            clock = clock,
            createdAtColumn = properties.createdAtColumn,
            updatedAtColumn = properties.updatedAtColumn,
        ),
    )
}
