package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ServiceCorePropertiesAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ServiceCorePropertiesAutoConfiguration::class.java))

    @Test
    fun `loads service-core properties - application tag defaults to unknown when build name is absent`() {
        contextRunner.run { ctx ->
            assertThat(ctx.environment.getProperty("management.metrics.tags.application"))
                .isEqualTo("unknown")
        }
    }

    @Test
    fun `application tag resolves to build name when build name is set`() {
        contextRunner
            .withPropertyValues("build.name=my-service")
            .run { ctx ->
                assertThat(ctx.environment.getProperty("management.metrics.tags.application"))
                    .isEqualTo("my-service")
            }
    }
}
