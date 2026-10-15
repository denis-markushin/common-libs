package org.dema.outbox

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import tools.jackson.databind.ObjectMapper
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.sql.DataSource
import kotlin.random.Random

/**
 * Boots the outbox autoconfiguration in a real Spring context. Verifies that
 * [OutboxPublisher] starts its own relay thread via [org.springframework.context.SmartLifecycle]
 * and that the bundled Liquibase changelog creates the outbox_events table.
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

    /**
     * Application whose only transaction manager leaves the outbox DataSource alone,
     * the way a KafkaTransactionManager does.
     */
    @Configuration
    class ForeignTransactions {
        @Bean
        fun dataSource(): DataSource = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)

        @Bean
        fun txManager(): PlatformTransactionManager = object : AbstractPlatformTransactionManager() {
            override fun doGetTransaction(): Any = Any()

            override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit

            override fun doCommit(status: DefaultTransactionStatus) = Unit

            override fun doRollback(status: DefaultTransactionStatus) = Unit
        }

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
        // Very high poll interval so the relay tick does not fire during the short test.
        .withPropertyValues("dema.outbox.topic=test.topic", "dema.outbox.poll-interval-ms=3600000")

    @Test
    fun `context loads and publisher runs its own relay lifecycle`() {
        runner.run { context ->
            val publisher = context.getBean<OutboxPublisher>()
            assertThat(publisher.isRunning).isTrue()
        }
    }

    @Test
    fun `context fails when topic is blank`() {
        ApplicationContextRunner()
            .withUserConfiguration(TestBeans::class.java)
            .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration::class.java))
            // no dema.outbox.topic set -> blank default
            .run { context ->
                assertThat(context.startupFailure).isNotNull()
            }
    }

    @Test
    fun `liquibase migrator auto-creates outbox_events`() {
        runner.run { context ->
            assertThat(context.startupFailure == null).isTrue()
            val ds = context.getBean<DataSource>()
            ds.connection.use { c ->
                val rs = c.createStatement().executeQuery(
                    "select count(*) from information_schema.tables where table_name = 'outbox_events'",
                )
                rs.next()
                assertThat(rs.getInt(1) == 1).isTrue()
            }
        }
    }

    @Test
    fun `service and publisher share a single OutboxStore bean`() {
        runner.run { context ->
            assertThat(context.getBeansOfType<OutboxStore>().size).isEqualTo(1)
        }
    }

    @Test
    fun `actuator registry exposes the outbox gauges`() {
        runner
            .withConfiguration(
                AutoConfigurations.of(
                    MetricsAutoConfiguration::class.java,
                    CompositeMeterRegistryAutoConfiguration::class.java,
                    SimpleMetricsExportAutoConfiguration::class.java,
                ),
            )
            .run { context ->
                assertThat(context.getBean<MeterRegistry>().find("outbox.relay.paused").gauge(), "relay pause invisible to the scraper")
                    .isNotNull()
            }
    }

    @Test
    fun `metrics back off without a meter registry`() {
        runner.run { context ->
            assertThat(context.containsBean("outboxMetrics"), "gauges registered with no registry to scrape them").isFalse()
        }
    }

    @Test
    fun `metrics back off when micrometer classes are filtered out`() {
        runner
            .withBean(SimpleMeterRegistry::class.java)
            .withClassLoader(FilteredClassLoader("io.micrometer"))
            .run { context ->
                assertThat(context.containsBean("outboxMetrics"), "starter demanded micrometer from a service without it").isFalse()
            }
    }

    @Test
    fun `relay keeps its lock for the whole batch whatever transaction manager the application has`() {
        var free: Boolean? = null
        ApplicationContextRunner()
            .withUserConfiguration(ForeignTransactions::class.java)
            .withConfiguration(
                AutoConfigurations.of(
                    OutboxAutoConfiguration::class.java,
                    OutboxLiquibaseAutoConfiguration::class.java,
                ),
            )
            .withPropertyValues("dema.outbox.topic=orders.${UUID.randomUUID()}", "dema.outbox.poll-interval-ms=3600000")
            .run { context ->
                val store = context.getBean<OutboxStore>()
                val kafka = context.getBean<KafkaTemplate<String, String>>()
                store.insert(UUID.randomUUID(), "Order", UUID.randomUUID(), "OrderClosed", """{"seat":${Random.nextInt()}}""")
                every { kafka.send(any(), any(), any()) } answers {
                    free = lock()
                    CompletableFuture.completedFuture(mockk(relaxed = true))
                }
                context.getBean<OutboxPublisher>().publish()
            }
        assertThat(free, "relay let go of its lock in the middle of a batch").isEqualTo(false)
    }

    private fun lock(): Boolean {
        val source = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)
        return TransactionTemplate(DataSourceTransactionManager(source)).execute { OutboxStore(JdbcTemplate(source)).lock() } == true
    }
}
