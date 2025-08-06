package org.dema.jooq.liquibase

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.DirectoryResourceAccessor
import org.jooq.DSLContext
import org.jooq.meta.postgres.PostgresDatabase
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class LiquibasePostgresTcDatabase : PostgresDatabase() {
    companion object {
        private var alreadyUpdated: AtomicBoolean = AtomicBoolean(false)
    }

    override fun create0(): DSLContext {
        if (alreadyUpdated.compareAndSet(false, true)) {
            val liquibaseChangelogFilePath =
                requireNotNull(properties["liquibaseChangelogFile"] as String) { "Missing 'liquibaseChangelogFile' property" }
            val liquibaseChangelog = File(liquibaseChangelogFilePath)
            Liquibase(
                liquibaseChangelog.name,
                DirectoryResourceAccessor(liquibaseChangelog.parentFile),
                JdbcConnection(connection),
            ).update()
        }
        return super.create0()
    }
}
