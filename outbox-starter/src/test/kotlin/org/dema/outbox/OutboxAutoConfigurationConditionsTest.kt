package org.dema.outbox

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.kafka.core.KafkaTemplate

/** Verifies the autoconfiguration backs off instead of forcing beans onto every consumer. */
class OutboxAutoConfigurationConditionsTest {
    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OutboxAutoConfiguration::class.java))

    @Test
    fun `backs off when dema outbox enabled is false`() {
        runner.withPropertyValues("dema.outbox.enabled=false").run { context ->
            assertThat(context.containsBean("outboxPublisher")).isEqualTo(false)
        }
    }

    @Test
    fun `backs off when kafka is not on the classpath`() {
        runner.withClassLoader(FilteredClassLoader(KafkaTemplate::class.java)).run { context ->
            assertThat(context.containsBean("outboxPublisher")).isEqualTo(false)
        }
    }

    @Test
    fun `master switch disables the liquibase migrator`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OutboxLiquibaseAutoConfiguration::class.java))
            .withPropertyValues("dema.outbox.enabled=false")
            .run { context ->
                assertThat(context.containsBean("outboxLiquibaseMigrator")).isEqualTo(false)
            }
    }
}
