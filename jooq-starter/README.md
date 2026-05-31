# jooq-starter

Spring Boot starter for jOOQ. Provides:

- `AbstractRepository` — base repository with CRUD utilities and upsert helpers.
- `JooqUtils` — pagination helpers that compute metadata in a single database roundtrip.
- `OffsetBasedPageRequest` — Spring `Pageable` implementation based on offset/limit.
- `TimestampsRecordListener` — auto-populates `created_at` / `updated_at` columns on `UpdatableRecord.store()`.

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
