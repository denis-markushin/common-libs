package org.dema.minio

import io.minio.MinioClient
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Wires a [MinioStorage] over the bucket named by [MinioProperties].
 *
 * Two [MinioClient] beans are built because a pre-signed URL must carry a host the
 * client can reach, which is rarely the host this service reaches. Building the URL
 * is local computation, so the second client never opens a connection.
 */
@AutoConfiguration
@ConditionalOnClass(MinioClient::class)
@EnableConfigurationProperties(MinioProperties::class)
class MinioAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["minioClient"])
    fun minioClient(properties: MinioProperties): MinioClient = client(properties.endpoint, properties)

    @Bean
    @ConditionalOnMissingBean(name = ["minioSigningClient"])
    fun minioSigningClient(properties: MinioProperties): MinioClient = client(properties.publicEndpoint, properties)

    @Bean
    @ConditionalOnMissingBean
    fun minioStorage(
        minioClient: MinioClient,
        minioSigningClient: MinioClient,
        properties: MinioProperties,
    ): MinioStorage = MinioStorage(minioClient, minioSigningClient, properties)

    /**
     * Creates the bucket after the context is up rather than while beans are built.
     *
     * A constructor that reaches the network turns an unreachable MinIO into a
     * context failure, which also means a build-time context refresh, such as an
     * AppCDS training run, cannot get past it.
     */
    @Bean
    @ConditionalOnProperty(prefix = "minio", name = ["ensure-bucket"], havingValue = "true", matchIfMissing = true)
    fun minioBucketInitializer(storage: MinioStorage): ApplicationRunner = ApplicationRunner { storage.ensureBucket() }

    private fun client(endpoint: String, properties: MinioProperties): MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(properties.accessKey, properties.secretKey)
        .build()
}
