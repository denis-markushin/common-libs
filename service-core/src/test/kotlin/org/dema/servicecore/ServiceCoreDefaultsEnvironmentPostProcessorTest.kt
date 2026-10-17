package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
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
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

class ServiceCoreDefaultsEnvironmentPostProcessorTest {

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
    @Timeout(60)
    fun `prometheus endpoint serves metrics over web`() {
        assertThat(scrape(), "scraper found no metrics on the prometheus endpoint").contains("# TYPE jvm_")
    }

    @Test
    @Timeout(60)
    fun `metrics carry the service name as application tag`() {
        val name = "svc-${UUID.randomUUID().toString().take(8)}"
        assertThat(scrape("spring.application.name=$name"), "dashboards cannot tell this service apart from the others")
            .contains("application=\"$name\"")
    }

    @Test
    fun `application tag falls back to unknown without a service name`() {
        val tag = defaults().getProperty("management.metrics.tags.application")
        assertThat(tag, "untagged service collides with a named one").isEqualTo("unknown")
    }

    @Test
    fun `logbook defaults write json`() {
        assertThat(defaults().getProperty("logbook.format.style"), "exchange log lost its json format").isEqualTo("json")
    }

    @Test
    fun `logbook defaults log only api paths`() {
        val path = defaults().getProperty("logbook.predicate.include[0].path")
        assertThat(path, "exchange log covers paths outside the api").isEqualTo("/api/**")
    }

    @Test
    fun `service properties win over service-core defaults`() {
        val environment = StandardEnvironment()
        val service = MapPropertySource("service", mapOf("management.endpoints.web.exposure.include" to "health"))
        environment.propertySources.addFirst(service)
        ServiceCoreDefaultsEnvironmentPostProcessor().postProcessEnvironment(environment, SpringApplication())
        assertThat(exposed("prometheus", environment), "service-core defaults overrode the service's own exposure").isFalse()
    }

    private fun defaults(): StandardEnvironment =
        StandardEnvironment().also { ServiceCoreDefaultsEnvironmentPostProcessor().postProcessEnvironment(it, SpringApplication()) }

    private fun exposed(endpoint: String, environment: StandardEnvironment = defaults()): Boolean =
        IncludeExcludeEndpointFilter(ExposableWebEndpoint::class.java, environment, "management.endpoints.web.exposure")
            .match(EndpointId.of(endpoint))

    private fun scrape(vararg properties: String): String =
        SpringApplicationBuilder(MetricsApplication::class.java)
            .properties("server.port=0", "spring.main.banner-mode=off", "logging.level.root=OFF", *properties)
            .run()
            .use { context ->
                val port = (context as WebServerApplicationContext).webServer!!.port
                HttpClient.newHttpClient().use { client ->
                    client.send(
                        HttpRequest.newBuilder(URI("http://localhost:$port/actuator/prometheus"))
                            .timeout(Duration.ofSeconds(10))
                            .build(),
                        HttpResponse.BodyHandlers.ofString(),
                    ).body()
                }
            }

    /**
     * Application that parses the Prometheus export ahead of every auto-configuration,
     * the order a real service with a long classpath ends up in.
     */
    @SpringBootConfiguration(proxyBeanMethods = false)
    @Import(MetricsAutoConfiguration::class, PrometheusMetricsExportAutoConfiguration::class)
    @EnableAutoConfiguration
    class MetricsApplication
}
