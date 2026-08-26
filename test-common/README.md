# Common test utils

Assertion helpers plus the Spring and Testcontainers scaffolding that every
service otherwise copies into its own test sources.

Everything beyond the assertion helpers is a `compileOnly` dependency, so a
project that only wants `isCloseToMillis` does not pull Spring or Testcontainers
into its build. Use the pieces you need; the classpath you already have for
integration tests supplies the rest.

## Assertions

`assertk` extensions for time types whose sub-millisecond precision survives a
round trip through code but not through PostgreSQL:

```kotlin
assertThat(record.createdAt).isEqualToMillis(expected)
assertThat(record.updatedAt).isCloseToMillis(expected, within = 50)
```

`String.withRandomSuffix()` builds fixture values that stay unique across a test
run without a shared counter.

## Security

`@WithMockJwt` authenticates a test with a stub JWT principal, and
`TestJwtDecoderConfig` accepts whatever token is presented. Together they let a
secured endpoint be exercised with no identity provider in the picture.

```kotlin
@Import(TestJwtDecoderConfig::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class AbstractIntegrationTest

class ProjectQueriesTest : AbstractIntegrationTest() {

    @Test
    @WithMockJwt(roles = ["SUPERVISOR"])
    fun `lists projects visible to a supervisor`() { ... }
}
```

Authorities are taken verbatim from `roles`, so pass whatever string the
authorization rules compare against. There is no default role: a test that needs
an authority says so.

## Containers

`KafkaInitializer` and `MinioInitializer` start a throwaway broker and object
store and publish the properties an application reads for them.

```kotlin
@ContextConfiguration(initializers = [KafkaInitializer::class, MinioInitializer::class])
abstract class AbstractIntegrationTest
```

Each container is held statically, so one broker and one MinIO serve every
context that names the initializer rather than one per test class. Testcontainers
reaps them when the JVM exits.

`MinioInitializer` publishes `minio.bucket` but does not create the bucket —
that belongs to the code under test, and a test asserting on bucket bootstrapping
needs it absent.
