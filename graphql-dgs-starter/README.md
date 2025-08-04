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
    implementation("org.dema.graphql:graphql-dgs-starter:<latest-version>")
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
    first: Int,
    after: String?,
    sort: UsersSort,
    dfe: DgsDataFetchingEnvironment
): Connection<User> {
    val pageable = RelayPageable(after, first, sort)
    val records = usersService.getUsers(pageable.orderByClauses, pageable.seekValues)

    return records.toConnection(pageable).map(::convert)
}
```

## License

[Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0)