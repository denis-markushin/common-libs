package org.dema.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@AutoConfiguration(after = [DataSourceAutoConfiguration::class, KafkaAutoConfiguration::class])
@EnableConfigurationProperties(OutboxProperties::class)
class OutboxAutoConfiguration {

    @Bean("outboxStore")
    @ConditionalOnMissingBean
    internal fun outboxStore(dataSource: DataSource): OutboxStore = OutboxStore(JdbcTemplate(dataSource))

    @Bean
    @ConditionalOnMissingBean
    internal fun outboxService(store: OutboxStore, mapper: ObjectMapper): OutboxService =
        OutboxService(store, mapper)

    @Bean
    @ConditionalOnMissingBean
    internal fun outboxPublisher(
        store: OutboxStore,
        kafka: KafkaTemplate<String, String>,
        txManager: PlatformTransactionManager,
        props: OutboxProperties,
    ): OutboxPublisher {
        require(props.topic.isNotBlank()) { "dema.outbox.topic must be configured" }
        return OutboxPublisher(store, kafka, props, TransactionTemplate(txManager))
    }
}
