package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.IncludeExcludeEndpointFilter
import org.springframework.boot.actuate.endpoint.EndpointId
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.core.env.StandardEnvironment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class ActuatorEnvironmentPostProcessorTest {

    @Test
    fun `heapdump is not exposed over web by default`() {
        assertThat(exposed("heapdump"), "heap dump full of application secrets is exposed over web").isFalse()
    }

    @Test
    fun `health is exposed over web by default`() {
        assertThat(exposed("health"), "health probes cannot reach the health endpoint").isTrue()
    }

    @Test
    fun `prometheus is exposed over web by default`() {
        assertThat(exposed("prometheus"), "metrics scraper cannot reach the prometheus endpoint").isTrue()
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

    private fun exposed(endpoint: String): Boolean =
        StandardEnvironment()
            .also { ActuatorEnvironmentPostProcessor().postProcessEnvironment(it, SpringApplication()) }
            .let { IncludeExcludeEndpointFilter(ExposableWebEndpoint::class.java, it, "management.endpoints.web.exposure") }
            .match(EndpointId.of(endpoint))

    /**
     * Application that parses the Prometheus export ahead of every auto-configuration,
     * the order a real service with a long classpath ends up in.
     */
    @SpringBootConfiguration(proxyBeanMethods = false)
    @Import(MetricsAutoConfiguration::class, PrometheusMetricsExportAutoConfiguration::class)
    @EnableAutoConfiguration
    class MetricsApplication
}
