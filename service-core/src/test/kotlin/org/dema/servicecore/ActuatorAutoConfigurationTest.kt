package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter
import org.springframework.boot.actuate.endpoint.EndpointId
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class ActuatorAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ActuatorAutoConfiguration::class.java))

    @Test
    fun `heapdump is not exposed over web by default`() {
        contextRunner.run { ctx ->
            assertThat(
                IncludeExcludeEndpointFilter(ExposableWebEndpoint::class.java, ctx.environment, "management.endpoints.web.exposure")
                    .match(EndpointId.of("heapdump")),
                "heap dump full of application secrets is exposed over web",
            ).isFalse()
        }
    }

    @Test
    fun `health is exposed over web by default`() {
        contextRunner.run { ctx ->
            assertThat(
                IncludeExcludeEndpointFilter(ExposableWebEndpoint::class.java, ctx.environment, "management.endpoints.web.exposure")
                    .match(EndpointId.of("health")),
                "health probes cannot reach the health endpoint",
            ).isTrue()
        }
    }

    @Test
    fun `prometheus is exposed over web by default`() {
        contextRunner.run { ctx ->
            assertThat(
                IncludeExcludeEndpointFilter(ExposableWebEndpoint::class.java, ctx.environment, "management.endpoints.web.exposure")
                    .match(EndpointId.of("prometheus")),
                "metrics scraper cannot reach the prometheus endpoint",
            ).isTrue()
        }
    }
}
