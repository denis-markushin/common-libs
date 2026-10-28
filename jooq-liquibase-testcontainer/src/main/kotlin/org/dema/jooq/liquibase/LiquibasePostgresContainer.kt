package org.dema.jooq.liquibase

import com.github.dockerjava.api.command.InspectContainerResponse
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.DirectoryResourceAccessor
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.File
import java.sql.DriverManager

/*
 * PostgreSQL container that applies a Liquibase changelog right after it starts, so jOOQ's plain
 * PostgresDatabase introspects a migrated schema. Reached through the Testcontainers JDBC URL
 * jdbc:tc:liquibasepostgres:<tag>:///<db>?changelog=<absolute path>; the changelog parameter is
 * removed before the query string reaches the PostgreSQL driver.
 */
class LiquibasePostgresContainer(
    tag: String,
    private val changelog: File,
) : PostgreSQLContainer("postgres:$tag") {

    @Suppress("DEPRECATION")
    override fun containerIsStarted(containerInfo: InspectContainerResponse) {
        super.containerIsStarted(containerInfo)
        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            Liquibase(changelog.name, DirectoryResourceAccessor(changelog.parentFile), JdbcConnection(connection)).update()
        }
    }

    override fun constructUrlForConnection(queryString: String): String {
        val kept = queryString.removePrefix("?").split("&").filter { it.isNotEmpty() && !it.startsWith("$CHANGELOG_PARAM=") }
        return super.constructUrlForConnection(if (kept.isEmpty()) "" else kept.joinToString("&", prefix = "?"))
    }

    companion object {
        const val DATABASE_TYPE = "liquibasepostgres"
        const val CHANGELOG_PARAM = "changelog"
    }
}
