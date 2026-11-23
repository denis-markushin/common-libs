package org.dema.minio

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.minio.GetPresignedObjectUrlArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.Result
import io.minio.messages.Item
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.random.Random

class MinioStorageTest {

    @Test
    fun `signs a presigned url with the client that carries the public endpoint`() {
        val signer = mockk<MinioClient>()
        val url = "https://files-${Random.nextInt(1000)}.example.org/survey/7.pdf"
        every { signer.getPresignedObjectUrl(any<GetPresignedObjectUrlArgs>()) } returns url
        assertThat(
            storage(mockk(), signer).presignedUrl("survey/7.pdf", "survey.pdf"),
            "a presigned url signed by the internal client leaks a host no browser can reach",
        ).isEqualTo(url)
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

    @Test
    fun `pulls no more objects than the caller consumes`() {
        val client = mockk<MinioClient>()
        var pulls = 0
        every { client.listObjects(any<ListObjectsArgs>()) } returns generateSequence {
            pulls++
            Result<Item>(object : Item("report-${Random.nextInt()}.csv") {})
        }.take(Random.nextInt(2, 50)).asIterable()
        storage(client, mockk()).list("reports-${Random.nextInt(1000)}/", Random.nextBoolean()).first()
        assertThat(
            pulls,
            "a listing must not drag the whole bucket into memory to hand out one object",
        ).isEqualTo(1)
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
