package org.dema.test.container

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.utility.DockerImageName

/**
 * Points a test context at a throwaway MinIO server.
 *
 * Publishes the `minio.*` properties an application reads for its endpoint and
 * credentials. The bucket is named but not created: creating it is the job of the
 * code under test, and a test that asserts on bucket bootstrapping needs it absent.
 *
 * Runs the community fork `pgsty/minio`: the upstream `minio/minio` repository is
 * gone from Docker Hub, so a runner without a cached image cannot pull it.
 */
class MinioInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(ctx: ConfigurableApplicationContext) {
        minio.start()
        TestPropertyValues
            .of(
                "minio.endpoint=${minio.s3URL}",
                "minio.publicEndpoint=${minio.s3URL}",
                "minio.accessKey=${minio.userName}",
                "minio.secretKey=${minio.password}",
                "minio.bucket=$BUCKET",
            )
            .applyTo(ctx.environment)
    }

    private companion object {
        const val BUCKET = "test-bucket"
        val minio = MinIOContainer(
            DockerImageName.parse("pgsty/minio:RELEASE.2026-08-04T00-00-00Z").asCompatibleSubstituteFor("minio/minio"),
        )
            .withUserName("testuser")
            .withPassword("testpass")
    }
}
