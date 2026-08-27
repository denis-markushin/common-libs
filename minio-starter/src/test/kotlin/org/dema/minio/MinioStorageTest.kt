package org.dema.minio

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class MinioStorageTest {

    @Test
    fun `signs a presigned url with the client that carries the public endpoint`() {
        val client = mockk<MinioClient>()
        val signer = mockk<MinioClient>()
        every { signer.getPresignedObjectUrl(any<GetPresignedObjectUrlArgs>()) } returns "https://public/x"
        storage(client, signer).presignedUrl("survey/7.pdf", "survey.pdf")
        verify(exactly = 0) {
            client.getPresignedObjectUrl(any<GetPresignedObjectUrlArgs>())
        }
    }

    @Test
    fun `addresses the configured bucket when signing`() {
        val signer = mockk<MinioClient>()
        val args = slot<GetPresignedObjectUrlArgs>()
        every { signer.getPresignedObjectUrl(capture(args)) } returns "https://public/x"
        storage(mockk(), signer).presignedUrl("survey/7.pdf", "survey.pdf")
        assertThat(
            args.captured.bucket(),
            "a presigned url must point at the configured bucket, not at whatever the key implies",
        ).isEqualTo("dumps-3f1a")
    }

    @Test
    fun `falls back to a binary content type when the caller knows none`() {
        val client = mockk<MinioClient>(relaxed = true)
        val args = slot<PutObjectArgs>()
        every { client.putObject(capture(args)) } returns mockk(relaxed = true)
        storage(client, mockk()).put("survey/7.pdf", ByteArrayInputStream(ByteArray(3)), 3, null)
        assertThat(
            args.captured.contentType(),
            "an unknown content type must not be sent as empty, browsers guess badly",
        ).isEqualTo("application/octet-stream")
    }

    @Test
    fun `keeps the content type the caller supplied`() {
        val client = mockk<MinioClient>(relaxed = true)
        val args = slot<PutObjectArgs>()
        every { client.putObject(capture(args)) } returns mockk(relaxed = true)
        storage(client, mockk()).put("survey/7.pdf", ByteArrayInputStream(ByteArray(3)), 3, "application/pdf")
        assertThat(
            args.captured.contentType(),
            "a known content type must survive to MinIO untouched",
        ).isEqualTo("application/pdf")
    }

    private fun storage(client: MinioClient, signer: MinioClient) = MinioStorage(
        client,
        signer,
        MinioProperties(
            endpoint = "http://minio.internal:9000",
            publicEndpoint = "https://files.example.org",
            accessKey = "ak-91",
            secretKey = "sk-2f",
            bucket = "dumps-3f1a",
        ),
    )
}
