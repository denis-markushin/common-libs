# MinIO starter

Object storage over one bucket, wired from configuration.

```kotlin
implementation("io.github.denis-markushin:minio-starter:x.x.x")
```

```yaml
minio:
  endpoint: ${MINIO_ENDPOINT}
  public-endpoint: ${MINIO_PUBLIC_ENDPOINT}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: ${MINIO_BUCKET}
```

```kotlin
@Service
class DocumentService(private val storage: MinioStorage) {

    fun upload(file: MultipartFile, key: String) =
        storage.put(key, file.inputStream, file.size, file.contentType)

    fun link(key: String, name: String) = storage.presignedUrl(key, name)
}
```

## Two endpoints, two clients

`endpoint` is where this service reaches MinIO. `public-endpoint` is the host
baked into pre-signed URLs, which has to be reachable by whoever follows the
link — usually a browser on the other side of a gateway, on a name the service
itself cannot resolve.

The starter builds a client for each. The signing client never opens a
connection: `getPresignedObjectUrl` is local computation, and the endpoint only
supplies the host it writes into the URL. Both are ordinary beans named
`minioClient` and `minioSigningClient`, overridable by declaring your own.

## Keys are strings

`MinioStorage` takes opaque keys. What separates a prefix from an object name is
the caller's convention, so a service that thinks in paths converts on its own
side rather than having a convention imposed here.

## Bucket creation

The bucket is created on startup when it does not exist, from an
`ApplicationRunner` rather than a bean constructor. Reaching the network while
beans are being built turns an unreachable MinIO into a context failure, and
blocks anything that refreshes a context without infrastructure — an AppCDS
training run at image build time, for instance.

Where the bucket is provisioned outside the application, or the credentials are
read-only, turn it off:

```yaml
minio:
  ensure-bucket: false
```
