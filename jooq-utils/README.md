# jooq-utils

Provides a way to integrate Spring Data with Jooq. Namely, allows you to get `org.springframework.data.domain.Page` as result of selecting
the data from DB using Jooq. As well as, to use `org.springframework.data.domain.Pageable` as input for repository methods.

## Samples

1. Create repository:
    ```kotlin
    import org.jooq.Condition
    import org.springframework.data.domain.Page
    import org.springframework.data.domain.Pageable
    import org.springframework.stereotype.Component
    import org.dema.Tables.USERS
    import org.dema.jooq.AbstractRepository

    @Component
    class UsersRepository : AbstractRepository<Users, UsersRecord>(table = USERS) {

        fun getAllBy(
            orderBy: Array<out OrderField<*>>,
            seekValues: Array<out Any>,
            limit: Int,
            vararg where: (Users) -> Condition,
        ): Result<UsersRecord> {
            return baseQuery(*where)
                .orderBy(*orderBy)
                .seek(*seekValues)
                .limit(limit)
                .fetch()
        }

        fun getPageBy(pageable: Pageable, condition: Condition): Page<UsersRecord> {
            val query = baseQuery({ condition })
            return paginate(dsl, query, pageable, USERS)
        }

        private fun baseQuery(vararg where: (Users) -> Condition): SelectConditionStep<UsersRecord> {
            return dsl.selectFrom(USERS)
                .where(foldConditions(where))
        }
    }
    ```

2. Use it:
    ```kotlin
    import org.jooq.Condition
    import org.jooq.impl.DSL.noCondition
    import org.springframework.data.domain.Page
    import org.springframework.data.domain.Pageable
    import org.springframework.stereotype.Service
    import org.springframework.core.convert.ConversionService
    import org.springframework.transaction.annotation.Transactional

    @Service
    @Transactional(readOnly = true)
    class UsersService(
        @Qualifier("mvcConversionService") private val cs: ConversionService,
        private val usersRepository: UsersRepository,
    ) {

        fun search(filter: UsersFilter, pageable: Pageable): UsersPage {
            val filterCondition: Condition = filter?.let(cs::convert) ?: noCondition()
            val userRecordsPage: Page<UsersRecord> = usersRepository.getPageBy(pageable, filterCondition)
            return cs.convert(userRecordsPage)!!
        }
    }
    ```
3. That`s it!