package org.dema.minio

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import io.minio.MinioClient
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class MinioAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(MinioAutoConfiguration::class.java))
        .withPropertyValues(
            "minio.endpoint=http://minio.internal:9000",
            "minio.public-endpoint=https://files.example.org",
            "minio.access-key=ak-91",
            "minio.secret-key=sk-2f",
            "minio.bucket=dumps-3f1a",
        )

    @Test
    fun `exposes storage over the configured bucket`() {
        runner.run { context ->
            assertThat(
                context.containsBean("minioStorage"),
                "a service on this starter must get storage without wiring clients itself",
            ).isTrue()
        }
    }

    @Test
    fun `builds a separate client for signing so the public endpoint is not used for traffic`() {
        runner.run { context ->
            assertThat(
                context.getBean("minioClient", MinioClient::class.java),
                "signing and traffic must not share a client, their endpoints differ",
            ).isNotEqualTo(context.getBean("minioSigningClient", MinioClient::class.java))
        }
    }

    @Test
    fun `creates the bucket on startup by default`() {
        runner.run { context ->
            assertThat(
                context.containsBean("minioBucketInitializer"),
                "a service pointed at an empty MinIO must come up working, not fail on first upload",
            ).isTrue()
        }
    }

    @Test
    fun `leaves the bucket alone when it is provisioned elsewhere`() {
        runner.withPropertyValues("minio.ensure-bucket=false").run { context ->
            assertThat(
                context.containsBean("minioBucketInitializer"),
                "read-only credentials must not be asked to create a bucket",
            ).isFalse()
        }
    }
}
