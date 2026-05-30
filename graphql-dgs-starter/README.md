# graphql-dgs-starter

Spring Boot GraphQL starter that integrates [Netflix DGS Framework](https://netflix.github.io/dgs/)
with [Jooq](https://www.jooq.org/) for building powerful and type-safe GraphQL APIs in Kotlin or Java.

This starter simplifies pagination, sorting, and cursor-based navigation using idiomatic Kotlin and Spring Boot
conventions.

## Features

* Cursor-based pagination (Relay-style)
* Sorting with safe enum-to-field mapping
* Base64-encoded cursors
* Type-safe pagination helpers built on top of Jooq

## Requirements

* Java 17+
* Spring Boot 3.x
* Kotlin (recommended)
* Netflix DGS Framework
* Jooq

## Setup

Add the dependency:

```kotlin
dependencies {
  implementation("io.github.denis-markushin:graphql-dgs-starter:<latest-version>")
}
```

## Usage

### 1. Define sorting enum and register mapping:

You can register mappings in a separate component:

```kotlin
enum class UsersSort {
  CREATED_AT_ASC,
  CREATED_AT_DESC,
}

@Component
class UsersOrderByRegistration {
  init {
    OrderByClausesMapping.register {
      key(UsersSort.CREATED_AT_ASC) {
        fields(USERS.CREATED_AT.asc())
      }
      key(UsersSort.CREATED_AT_DESC) {
        fields(USERS.CREATED_AT.desc())
      }
    }
  }
}
```

Or directly in the service layer:

```kotlin
@Service
class UsersService(
  private val cs: ConversionService,
  private val usersRepo: UsersRepo,
) {
  init {
    OrderByClausesMapping.register {
      key(UsersSort.CREATED_AT_DESC).fields(USERS.CREATED_AT.desc())
    }
  }

  fun getAll(
    relayPageable: RelayPageable,
    filter: UsersFilter?,
  ): Connection<UsersRecord> {
    val condition: Condition = cs.convert(filter) ?: noCondition()
    return usersRepo.getAllBy(relayPageable = relayPageable, where = condition).toConnection(relayPageable)
  }
}
```

Both approaches are valid — either co-locate the mapping with the service that uses it, or centralize all mappings in a
shared config component.

**When to choose which:**

* **Register in service:**

  * ✅ Keeps sorting logic close to its usage
  * ✅ Useful if the mapping is only relevant to one service
  * ❌ Might lead to duplication if reused elsewhere

* **Register in shared config:**

  * ✅ Promotes reuse and single source of truth
  * ✅ Easier to test mappings in isolation
  * ❌ Requires discipline to keep it in sync with evolving business logic

For small projects or isolated cases, inline registration in services can be perfectly fine.
For larger codebases or when sorting logic is shared across multiple layers, centralizing it is often more maintainable.

### 2. Use `RelayPageable` and generate cursors:

The extension function `toConnection(RelayPageable)` transforms a list of Jooq `Record` objects into a GraphQL
`Connection<T>` result that includes Relay-compliant pagination metadata (edges and pageInfo).
It uses the sorting and cursor fields from `RelayPageable` to calculate page boundaries and encode cursors.

```kotlin
@DgsData(parentType = "Query")
fun users(
  first: Int = 10,
  after: String?,
  sort: UsersSort = UsersSort.CREATED_AT_DESC,
  filter: UsersFilter?,
): Connection<User> {
  val pageable = RelayPageable(after, first, sort)
  return usersService.getAll(relayPageable, filter)
}

@Service
class UsersService(
  private val cs: ConversionService,
  private val usersRepo: UsersRepo,
) {

  init {
    OrderByClausesMapping.register {
      key(UsersSort.CREATED_AT_DESC).fields(USERS.CREATED_AT.desc())
    }
  }

  fun getAll(
    relayPageable: RelayPageable,
    filter: UsersFilter?,
  ): Connection<User> {
    val condition: Condition = cs.convert(filter) ?: noCondition()
    return usersRepo
      .getAllBy(relayPageable = relayPageable, where = condition)
      .toConnection(relayPageable)
      .map(cs::convert)
  }
}
```

## AbstractBatchLoader for DGS / DataLoader

A lightweight base class for **mapped** DataLoader batch loading in Netflix DGS.
It delegates the work to a function you provide and runs it asynchronously on a supplied `Executor`.

```kotlin
abstract class AbstractBatchLoader<K, V>(
  private val executor: Executor,
  private val loaderFn: (Set<K>) -> Map<K, V>
) : MappedBatchLoader<K, V> {

  override fun load(keys: Set<K>): CompletionStage<Map<K, V>> =
    if (keys.isEmpty()) CompletableFuture.completedFuture(emptyMap())
    else CompletableFuture.supplyAsync({ loaderFn(keys) }, executor)
}
```

## Mutation error handling

GraphQL mutation responses follow [rule 6.6.4](https://github.com/nodkz/conf-talks/tree/master/articles/graphql/schema-design#rule-6.6.4):
when the client selects the typed `error` field on the mutation payload, the
resolver returns a typed error implementing `ErrorInterface`; otherwise the
exception is reported via the top-level `errors[]` array. This starter
automates the branch via `dfe.resolveMutation { ... }` and provides a
pluggable chain of mappers that turn exceptions into typed errors. When
the client does NOT select the typed `error` field, the exception is
rethrown and DGS's default error handling emits the matching `ErrorType`
(e.g. `NOT_FOUND`, `PERMISSION_DENIED`) into the top-level `errors[]`.

### Built-in error types

| GraphQL type              | Kotlin class                                         | Intended use                                                       |
|---------------------------|------------------------------------------------------|--------------------------------------------------------------------|
| `NotFoundError`           | `org.dema.graphql.dgs.error.NotFoundError`           | Requested entity does not exist.                                   |
| `ValidationError`         | `org.dema.graphql.dgs.error.ValidationError`         | Input fails validation (`path`, `value` optional).                 |
| `ConflictError`           | `org.dema.graphql.dgs.error.ConflictError`           | Operation conflicts with current state (`reason` is a code).       |
| `UnauthorizedError`       | `org.dema.graphql.dgs.error.UnauthorizedError`       | Caller is not authenticated.                                       |
| `ForbiddenError`          | `org.dema.graphql.dgs.error.ForbiddenError`          | Caller lacks permission for the operation.                         |
| `ServiceUnavailableError` | `org.dema.graphql.dgs.error.ServiceUnavailableError` | Service is temporarily unavailable (`retryAfterSeconds` optional). |
| `RuntimeError`            | `org.dema.graphql.dgs.error.RuntimeError`            | Fallback for any unclassified exception.                           |

### Built-in exception types

| Exception                                                                                              | Default error produced    |
|--------------------------------------------------------------------------------------------------------|---------------------------|
| `org.dema.graphql.dgs.exception.EntityNotFoundException`                                               | `NotFoundError`           |
| `com.netflix.graphql.dgs.exceptions.DgsEntityNotFoundException` (legacy fallback)                      | `NotFoundError`           |
| `org.dema.graphql.dgs.exception.DomainValidationException`                                             | `ValidationError`         |
| `org.dema.graphql.dgs.exception.ConflictException`                                                     | `ConflictError`           |
| `org.dema.graphql.dgs.exception.UnauthorizedException`                                                 | `UnauthorizedError`       |
| `org.springframework.security.core.AuthenticationException` (when Spring Security is on the classpath) | `UnauthorizedError`       |
| `org.dema.graphql.dgs.exception.ForbiddenException`                                                    | `ForbiddenError`          |
| `org.springframework.security.access.AccessDeniedException` (when Spring Security is on the classpath) | `ForbiddenError`          |
| `org.dema.graphql.dgs.exception.ServiceUnavailableException`                                           | `ServiceUnavailableError` |
| anything else                                                                                          | `RuntimeError`            |

### Setup

1. Add the starter dependency (see the top of this README).
2. Extend the DGS `typeMapping` so codegen does NOT regenerate the starter
   error types into your consumer package:

    ```kotlin
    platform {
        spring {
            netflixDgs {
                useNetflixDgs = true
                typeMapping.putAll(
                    mapOf(
                        "ErrorInterface"    to "org.dema.graphql.dgs.error.ErrorInterface",
                        "NotFoundError"     to "org.dema.graphql.dgs.error.NotFoundError",
                        "ValidationError"   to "org.dema.graphql.dgs.error.ValidationError",
                        "ConflictError"     to "org.dema.graphql.dgs.error.ConflictError",
                        "UnauthorizedError"      to "org.dema.graphql.dgs.error.UnauthorizedError",
                        "ForbiddenError"         to "org.dema.graphql.dgs.error.ForbiddenError",
                        "ServiceUnavailableError" to "org.dema.graphql.dgs.error.ServiceUnavailableError",
                        "RuntimeError"           to "org.dema.graphql.dgs.error.RuntimeError",
                    ),
                )
            }
        }
    }
    ```

3. Inject `MutationResolver` into each `@DgsComponent` mutation class and
   bring the extension into scope via Kotlin interface delegation.

### Minimal usage

```kotlin
@DgsComponent
class ProjectMutations(
  mutationResolver: MutationResolver,
  private val projectService: ProjectService,
) : MutationResolver by mutationResolver {

  @DgsMutation
  fun createProject(input: CreateProjectInput, dfe: DataFetchingEnvironment): CreateProjectResult =
    when (val r = dfe.resolveMutation { projectService.create(input) }) {
      is MutationOutcome.Success -> CreateProjectResult(record = { r.value }, error = { null })
      is MutationOutcome.Failure -> CreateProjectResult(record = { null }, error = { r.error })
    }
}
```

### Extending the mapper chain

#### 1. Subclass a built-in exception (preferred)

If your domain error fits one of the built-in categories, subclass the matching
exception — no custom mapper required. The default mapper picks it up:

```kotlin
class ContractorAlreadyAssignedException(
  val projectId: UUID,
) : ConflictException(
  message = "Contractor already assigned to project $projectId",
  reason = "CONTRACTOR_ALREADY_ASSIGNED",
  entityType = "Project",
  entityId = projectId,
)
```

#### 2. Register a custom mapper

When the exception does not fit any built-in hierarchy, or when the
produced error payload differs from the defaults, register a
`GraphQLErrorMapper` bean. Position it relative to the built-in mappers via
their named `ORDER` constants:

```kotlin
@Component
class QuotaExceededMapper : GraphQLErrorMapper, Ordered {
  override fun getOrder(): Int = ConflictErrorMapper.ORDER + 10

  override fun map(e: Throwable): ErrorInterface? = when (e) {
    is QuotaExceededException -> ConflictError(
      message = e.message ?: "Quota exceeded",
      reason = "QUOTA_EXCEEDED",
    )
    else -> null
  }
}
```

### Mapper ordering reference

Use named anchors, not magic numbers, when positioning custom mappers.

| Constant                              | Value |
|---------------------------------------|-------|
| `NotFoundErrorMapper.ORDER`           | `100` |
| `ConflictErrorMapper.ORDER`           | `200` |
| `ValidationErrorMapper.ORDER`         | `300` |
| `UnauthorizedErrorMapper.ORDER`       | `400` |
| `SpringSecurityAuthErrorMapper.ORDER` | `450` |
| `ForbiddenErrorMapper.ORDER`          | `500` |
| `ServiceUnavailableErrorMapper.ORDER` | `600` |

### Overriding a default mapper

Each built-in mapper is registered with `@ConditionalOnMissingBean(name = "...")`.
Supplying a bean with the same name fully replaces the default:

```kotlin
@Configuration
class CustomErrorConfig {
  @Bean
  fun notFoundErrorMapper(): NotFoundErrorMapper = MyCustomNotFoundMapper()
}
```

> **Replacing `CompositeGraphQLErrorMapper`:** if you supply your own
> `compositeGraphQLErrorMapper` bean, remember to inject the full
> `List<GraphQLErrorMapper>` so consumer and starter mappers stay in
> the chain. Order is preserved by Spring's `OrderComparator` over
> `Ordered.getOrder()` values.

### Adding a brand-new `ErrorInterface` subtype

> **No manual `@JsonSubTypes` registration.** Any class on the consumer's
> Spring Boot auto-configuration packages that implements
> `ErrorInterface` is registered with Jackson automatically — the
> `__typename` discriminator round-trips without further setup.

1. Add the type to your consumer schema:

    ```graphql
    type QuotaExceededError implements ErrorInterface @shareable {
        message: String!
        quotaName: String!
    }
    ```

2. Hand-write (or let DGS codegen produce) the Kotlin class implementing
   `org.dema.graphql.dgs.error.ErrorInterface`.
3. Register a `GraphQLErrorMapper` bean that returns it.

### Adding a brand-new exception type to the starter (for contributors)

1. Add the exception class under `org.dema.graphql.dgs.exception`.
2. Add the error `data class` under `org.dema.graphql.dgs.error` and, if the
   error is wire-visible, the matching GraphQL type in
   `src/main/resources/schema/common-errors.graphqls`.
3. Add a default mapper under `org.dema.graphql.dgs.errormapper` with its
   own `companion object const val ORDER`.
4. Register the bean in `GraphQLErrorAutoConfiguration` with
   `@ConditionalOnMissingBean(name = "...")`.
5. Extend the tests.
6. Update this README section.

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)
