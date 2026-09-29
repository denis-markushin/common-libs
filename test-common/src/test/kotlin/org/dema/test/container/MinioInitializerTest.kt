package org.dema.test.container

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.context.support.GenericApplicationContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class MinioInitializerTest {

    @Test
    fun `publishes the endpoint of a live minio server`() {
        val ctx = GenericApplicationContext()
        MinioInitializer().initialize(ctx)
        val status = HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create("${ctx.environment.getProperty("minio.endpoint")}/minio/health/live"))
                    .timeout(Duration.ofSeconds(10))
                    .build(),
                HttpResponse.BodyHandlers.discarding(),
            )
            .statusCode()
        assertThat(status, "published minio endpoint does not answer as a live server").isEqualTo(200)
    }
}
