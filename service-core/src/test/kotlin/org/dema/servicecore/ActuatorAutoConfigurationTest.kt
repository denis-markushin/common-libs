package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ActuatorAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ActuatorAutoConfiguration::class.java))

    @Test
    fun `loads actuator properties - exposes all endpoints`() {
        contextRunner.run { ctx ->
            assertThat(ctx.environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("*")
        }
    }
}
