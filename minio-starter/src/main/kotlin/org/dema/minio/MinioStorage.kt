package org.dema.minio

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.ListObjectsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.http.Method
import io.minio.messages.Item
import org.springframework.http.ContentDisposition
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Reads and writes objects in one bucket.
 *
 * Two clients rather than one: [client] speaks HTTP to the endpoint this service
 * reaches, while [signer] only computes signatures locally and exists to bake a
 * client-reachable host into a pre-signed URL. Nothing is sent through [signer].
 *
 * Keys are opaque strings. A caller that thinks in paths converts on its own side,
 * since what separates a prefix from a name is the caller's convention, not MinIO's.
 */
class MinioStorage(
    private val client: MinioClient,
    private val signer: MinioClient,
    private val properties: MinioProperties,
) {

    fun put(key: String, data: InputStream, size: Long, contentType: String?) {
        client.putObject(
            PutObjectArgs.builder()
                .bucket(properties.bucket)
                .`object`(key)
                .stream(data, size, -1)
                .contentType(contentType ?: DEFAULT_CONTENT_TYPE)
                .build(),
        )
    }

    fun get(key: String): InputStream = client.getObject(
        GetObjectArgs.builder()
            .bucket(properties.bucket)
            .`object`(key)
            .build(),
    )

    fun remove(key: String) {
        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(properties.bucket)
                .`object`(key)
                .build(),
        )
    }

    /**
     * Lists objects under [prefix], or the whole bucket when it is empty.
     *
     * Non-recursive listing stops at the first delimiter, so it reports what looks
     * like one directory level rather than every key beneath it.
     */
    fun list(prefix: String = "", recursive: Boolean = true): List<Item> = client.listObjects(
        ListObjectsArgs.builder()
            .bucket(properties.bucket)
            .prefix(prefix)
            .recursive(recursive)
            .build(),
    ).map { it.get() }

    /**
     * Builds a URL that lets its holder download [key] without credentials until it expires.
     *
     * [downloadName] is what the browser will call the saved file, independent of the key.
     */
    fun presignedUrl(key: String, downloadName: String, ttl: Duration = DEFAULT_TTL): String =
        signer.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .bucket(properties.bucket)
                .`object`(key)
                .method(Method.GET)
                .expiry(ttl.seconds.toInt())
                .extraQueryParams(
                    mapOf(
                        "response-content-disposition" to
                            ContentDisposition.inline().filename(downloadName, StandardCharsets.UTF_8).build().toString(),
                    ),
                )
                .build(),
        )

    internal fun ensureBucket() {
        val exists = client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket).build())
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket).build())
        }
    }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "application/octet-stream"
        val DEFAULT_TTL: Duration = Duration.ofMinutes(5)
    }
}
