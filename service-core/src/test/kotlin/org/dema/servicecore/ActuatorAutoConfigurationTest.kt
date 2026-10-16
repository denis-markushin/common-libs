package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter
import org.springframework.boot.actuate.endpoint.EndpointId
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.web.server.context.WebServerApplicationContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

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

    @Test
    fun `prometheus endpoint serves metrics over web`() {
        SpringApplicationBuilder(MetricsApplication::class.java)
            .properties("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF")
            .run()
            .use { context ->
                val port = (context as WebServerApplicationContext).webServer!!.port
                val response = HttpClient.newHttpClient().use { client ->
                    client.send(
                        HttpRequest.newBuilder(URI("http://localhost:$port/actuator/prometheus"))
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    )
                }
                assertThat(response.body(), "scraper found no metrics on the prometheus endpoint").contains("# TYPE jvm_")
            }
    }

    /**
     * Bare application that boots every auto-configuration on the test classpath,
     * so the request reaches the same endpoints a service built on service-core serves.
     */
    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    class MetricsApplication
}
