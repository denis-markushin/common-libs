package org.dema.outbox

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import javax.sql.DataSource
import kotlin.time.TimeSource

@AutoConfiguration(
    after = [DataSourceAutoConfiguration::class, KafkaAutoConfiguration::class],
    afterName = [
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
    ],
)
@ConditionalOnClass(KafkaTemplate::class, DataSource::class)
@ConditionalOnProperty(prefix = "dema.outbox", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OutboxProperties::class)
class OutboxAutoConfiguration {

    @Bean("outboxStore")
    @ConditionalOnMissingBean
    internal fun outboxStore(dataSource: DataSource): OutboxStore = OutboxStore(JdbcTemplate(dataSource))

    @Bean("outboxService")
    @ConditionalOnMissingBean
    internal fun outboxService(store: OutboxStore, mapper: ObjectMapper): OutboxService =
        OutboxService(store, mapper)

    @Bean("outboxPublisher")
    @ConditionalOnMissingBean
    internal fun outboxPublisher(
        store: OutboxStore,
        kafka: KafkaTemplate<String, String>,
        dataSource: DataSource,
        props: OutboxProperties,
    ): OutboxPublisher {
        require(props.topic.isNotBlank()) { "dema.outbox.topic must be configured" }
        val tx = TransactionTemplate(DataSourceTransactionManager(dataSource))
        return OutboxPublisher(store, kafka, props, tx, TimeSource.Monotonic)
    }

    /** Gauges for services that ship a Micrometer registry, e.g. through Spring Boot Actuator; Micrometer stays optional. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MeterRegistry::class)
    @ConditionalOnBean(MeterRegistry::class)
    internal class Metrics {

        @Bean("outboxMetrics")
        @ConditionalOnMissingBean
        internal fun outboxMetrics(store: OutboxStore, publisher: OutboxPublisher, props: OutboxProperties): OutboxMetrics =
            OutboxMetrics(store, publisher, props, TimeSource.Monotonic)
    }
}
