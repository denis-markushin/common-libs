# jooq-starter

- [Usage](#usage)
  - [Add dependency](#add-dependency)
  - [Audit actors](#audit-actors)
  - [Repository](#repository)
  - [Service](#service)

Spring Boot starter for jOOQ. Provides:

- `AbstractRepository` — base repository with CRUD utilities and upsert helpers.
- `JooqUtils` — pagination helpers that compute metadata in a single database roundtrip.
- `OffsetBasedPageRequest` — Spring `Pageable` implementation based on offset/limit.
- `TimestampsRecordListener` — auto-populates `created_at` / `updated_at` columns on `UpdatableRecord.store()`. Supports `LocalDateTime`, `OffsetDateTime` and `Instant` columns.
- `AbstractRepository.upsert` — also populates the timestamp columns; on `on conflict do update` the `created` column keeps its stored value while `updated` is refreshed.
- `AuditRecordListener` — auto-populates `created_by` / `updated_by` text columns with the login of the authenticated user. Requires `spring-security-core` and `spring-security-oauth2-jose` on the classpath.

## Usage

### Add dependency

```kotlin
implementation("io.github.denis-markushin:jooq-starter:x.x.x")
```

The timestamps listener is enabled by default. Disable or reconfigure via properties:

```yaml
dema:
  jooq:
    timestamps:
      enabled: true                       # default
      created-at-column: created_at       # default
      updated-at-column: updated_at       # default
```

### Audit actors

Each column is handled on its own: a table opts in per column by having a text column of that name.
A column of another type, or a missing one, is left alone.

The columns must be nullable with no default, and the library owns them.
A system write stores an explicit `null`, so a `not null` column fails and a default is bypassed.
A service that fills these columns itself must set `dema.jooq.audit.enabled: false` or rename the columns, service-wide.

Requires `spring-security-core` and `spring-security-oauth2-jose` on the classpath; without them audit stays off and everything else works.

The actor is the `preferred_username` claim of the JWT principal, then its subject; without a JWT, the authentication name.
A write without an authenticated user stores `null`, and clears `updated_by`, so a system write is never credited to the previous user.
`created_by` is set on a new row only: a row counts as new by its primary key, so rows older than the column stay empty.

`store()`, `insertOnConflictDoNothing` and `upsert` fill the columns. `actor()` covers custom statements, which set them themselves:

```kotlin
dsl.update(USERS)
    .set(USERS.DELETED_AT, moment)
    .set(USERS.UPDATED_BY, actor())
    .where(condition)
    .execute()
```

Still uncovered and left to the caller: `record.insert()`, `record.update()`, `record.merge()`, `store(fields)` and `batchInsert`.
`copy()` keeps the author of the source row: `created_by` is set only while empty, and the copy carries the value of its source.

```yaml
dema:
  jooq:
    audit:
      enabled: true                  # default
      created-by-column: created_by  # default
      updated-by-column: updated_by  # default
```

### Repository

```kotlin
@Component
class UsersRepository : AbstractRepository<Users, UsersRecord>(USERS) {
    fun getPageBy(pageable: Pageable, condition: Condition): Page<UsersRecord> {
        val query = baseQuery({ condition })
        return JooqUtils.paginate(dsl, query, pageable, USERS)
    }

    private fun baseQuery(vararg where: (Users) -> Condition): SelectConditionStep<UsersRecord> {
        return dsl.selectFrom(USERS).where(foldConditions(where))
    }
}
```

### Service

```kotlin
@Service
@Transactional(readOnly = true)
class UsersService(
    @Qualifier("mvcConversionService") private val cs: ConversionService,
    private val usersRepository: UsersRepository,
) {
    fun search(filter: UsersFilter?, pageable: Pageable): UsersPage {
        val condition: Condition = filter?.let(cs::convert) ?: noCondition()
        val page: Page<UsersRecord> = usersRepository.getPageBy(pageable, condition)
        return cs.convert(page)!!
    }
}
```

That's it!
