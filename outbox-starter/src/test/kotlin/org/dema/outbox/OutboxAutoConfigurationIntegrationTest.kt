package org.dema.outbox

import assertk.assertThat
import assertk.assertions.isTrue
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import javax.sql.DataSource

/**
 * Boots the outbox autoconfiguration in a REAL Spring [ApplicationContextRunner] context so that
 * the @Transactional CGLIB proxy of [OutboxPublisher] is actually created. If [OutboxPublisher]
 * were final, CGLIB proxying fails and `context.startupFailure` is non-null — making the first
 * test fail. This is the regression guard for the "Cannot subclass final class" defect.
 */
@Testcontainers
class OutboxAutoConfigurationIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17.5-alpine")
    }

    @Configuration
    class TestBeans {
        @Bean
        fun dataSource(): DataSource = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)

        @Bean
        fun txManager(ds: DataSource): PlatformTransactionManager = DataSourceTransactionManager(ds)

        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()

        @Bean
        fun kafkaTemplate(): KafkaTemplate<String, String> = mockk(relaxed = true)
    }

    private val runner = ApplicationContextRunner()
        .withUserConfiguration(TestBeans::class.java)
        .withConfiguration(
            AutoConfigurations.of(
                OutboxAutoConfiguration::class.java,
                OutboxLiquibaseAutoConfiguration::class.java,
            ),
        )
        // Very high poll interval so the @Scheduled tick does not fire during the short test.
        .withPropertyValues("dema.outbox.topic=test.topic", "dema.outbox.poll-interval-ms=3600000")

    @Test
    fun `context loads with outbox beans and transactional proxy`() {
        runner.run { context ->
            assertThat(context.startupFailure == null).isTrue()
            assertThat(context.getBeanNamesForType(OutboxService::class.java).isNotEmpty()).isTrue()
            assertThat(context.getBeanNamesForType(OutboxPublisher::class.java).isNotEmpty()).isTrue()
            val publisher = context.getBean(OutboxPublisher::class.java)
            assertThat(AopUtils.isAopProxy(publisher)).isTrue()
        }
    }

    @Test
    fun `context fails when topic is blank`() {
        ApplicationContextRunner()
            .withUserConfiguration(TestBeans::class.java)
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration::class.java))
            // no dema.outbox.topic set -> blank default
            .run { context ->
                assertThat(context.startupFailure != null).isTrue()
            }
    }

    @Test
    fun `liquibase migrator auto-creates outbox_events`() {
        runner.run { context ->
            assertThat(context.startupFailure == null).isTrue()
            val ds = context.getBean(DataSource::class.java)
            ds.connection.use { c ->
                val rs = c.createStatement().executeQuery(
                    "select count(*) from information_schema.tables where table_name = 'outbox_events'",
                )
                rs.next()
                assertThat(rs.getInt(1) == 1).isTrue()
            }
        }
    }
}
