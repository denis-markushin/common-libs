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
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@AutoConfiguration(after = [DataSourceAutoConfiguration::class, KafkaAutoConfiguration::class])
@EnableConfigurationProperties(OutboxProperties::class)
@EnableScheduling
@EnableTransactionManagement
class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun outboxService(dataSource: DataSource, mapper: ObjectMapper): OutboxService =
        OutboxService(OutboxStore(JdbcTemplate(dataSource)), mapper)

    @Bean
    @ConditionalOnMissingBean
    fun outboxPublisher(
        dataSource: DataSource,
        kafka: KafkaTemplate<String, String>,
        props: OutboxProperties,
    ): OutboxPublisher {
        require(props.topic.isNotBlank()) { "dema.outbox.topic must be configured" }
        return OutboxPublisher(OutboxStore(JdbcTemplate(dataSource)), kafka, props)
    }
}
