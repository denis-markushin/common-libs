package org.dema.minio

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "minio")
data class MinioProperties(
    /** Endpoint this service reaches MinIO on, used for every upload, download, listing and removal. */
    @NotBlank val endpoint: String,
    /** Endpoint baked into pre-signed URLs. Must be reachable by the client following the URL, not necessarily by this service. */
    @NotBlank val publicEndpoint: String,
    /** Access key presented to MinIO. */
    @NotBlank val accessKey: String,
    /** Secret key presented to MinIO. */
    @NotBlank val secretKey: String,
    /** Bucket every operation of [MinioStorage] addresses. */
    @NotBlank val bucket: String,
    /** Whether to create [bucket] on startup when it does not exist. Turn off where the bucket is provisioned outside the application. */
    val ensureBucket: Boolean = true,
)
