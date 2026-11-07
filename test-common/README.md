# Common test utils

- [Assertions](#assertions)
- [Security](#security)
- [Containers](#containers)
- [Concurrency](#concurrency)
- [Checks](#checks)

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
needs it absent. It runs the community fork `pgsty/minio`, since the upstream
`minio/minio` repository is no longer on Docker Hub.

## Concurrency

`Race.run` starts one action from several threads at once, behind a barrier, and
hands each thread its index. The threads act as the user of the test, so code that
reads the security context sees the same caller from every thread.

`Outcome.of` reduces a call to a line a test can compare: `SUCCESS`, the `reason`
of a `ConflictException` or the class name of any other exception. Sorted, the
outcomes of a race make one assertion:

```kotlin
val outcomes = Race.run(8) { Outcome.of { positionService.create(input()) } }
assertThat(outcomes.sorted()).isEqualTo(List(7) { "DUPLICATE_NAME" } + "SUCCESS")
```

`Outcome` reads `ConflictException` from `graphql-dgs-starter`: without it on the
test classpath, a failed call ends in `NoClassDefFoundError`.

## Checks

Architecture checks a service runs against itself. Each is an interface whose
default methods are JUnit tests, so a test class that already extends the
integration base of the service picks them up by implementing it; the
`ApplicationContext` arrives as a test method parameter from `SpringExtension`.

```kotlin
class SchemaConventionTest : SchemaConvention

class ResolverGuardTest : AbstractIntegrationTest(), ResolverGuard

class ActuatorExposureTest : AbstractIntegrationTest(), ActuatorExposure
```

- `SchemaConvention` holds the GraphQL schema under `src/main/resources/schema`
  against the conventions of `Deviations`: bare `*Id` fields in output types,
  plain lists in `*Queries` namespaces, missing descriptions. Known deviations sit
  in `src/test/resources/schema-debt.txt`, one per line; a new deviation and a debt
  line with nothing to forgive both fail. Override `schema` or `debt` to look
  elsewhere.
- `ResolverGuard` requires `@PreAuthorize` on every DGS resolver class and on every
  `@DgsEntityFetcher` method, and rejects any guard that lets an anonymous caller
  in. It suits a service whose `/graphql` is open and guarded by method security.
- `ActuatorExposure` fails when the actuator maps any web endpoint beyond `health`
  and `prometheus`.
