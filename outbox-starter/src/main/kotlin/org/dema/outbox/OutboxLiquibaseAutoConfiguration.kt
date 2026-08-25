package org.dema.outbox

import liquibase.Contexts
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration
import org.springframework.context.annotation.Bean
import javax.sql.DataSource

@AutoConfiguration(after = [DataSourceAutoConfiguration::class, LiquibaseAutoConfiguration::class])
@ConditionalOnProperty(name = ["dema.outbox.enabled", "dema.outbox.liquibase.enabled"], matchIfMissing = true, havingValue = "true")
class OutboxLiquibaseAutoConfiguration {

    @Bean
    fun outboxLiquibaseMigrator(dataSource: DataSource): OutboxLiquibaseMigrator =
        OutboxLiquibaseMigrator(dataSource)
}

/**
 * Applies the outbox changelog on startup WITHOUT registering a [liquibase.integration.spring.SpringLiquibase] bean,
 * so Spring Boot's primary Liquibase auto-configuration (the consumer's own migrations)
 * is not disabled by @ConditionalOnMissingBean(SpringLiquibase).
 */
class OutboxLiquibaseMigrator(
    private val dataSource: DataSource,
) : InitializingBean {
    override fun afterPropertiesSet() {
        dataSource.connection.use { conn ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(conn))
            Liquibase(CHANGELOG, ClassLoaderResourceAccessor(), database).use { lb ->
                lb.update(Contexts(CONTEXT))
            }
        }
    }

    private companion object {
        const val CHANGELOG = "liquibase/outbox/outbox-changelog.sql"
        const val CONTEXT = "outbox"
    }
}
