package org.dema.jooq.liquibase

import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.JdbcDatabaseContainerProvider
import org.testcontainers.jdbc.ConnectionUrl
import java.io.File

/*
 * Resolves jdbc:tc:liquibasepostgres:<tag>:///... URLs to a LiquibasePostgresContainer, the way
 * Testcontainers' own pgvector and timescaledb providers resolve theirs. Registered through
 * META-INF/services so the JDBC driver finds it on the classpath.
 */
class LiquibasePostgresContainerProvider : JdbcDatabaseContainerProvider() {

    override fun supports(databaseType: String): Boolean = databaseType == LiquibasePostgresContainer.DATABASE_TYPE

    override fun newInstance(tag: String): JdbcDatabaseContainer<*> =
        throw UnsupportedOperationException("${LiquibasePostgresContainer.DATABASE_TYPE} needs the ${LiquibasePostgresContainer.CHANGELOG_PARAM} URL parameter")

    override fun newInstance(url: ConnectionUrl): JdbcDatabaseContainer<*> {
        val tag = url.imageTag.orElseThrow { IllegalArgumentException("Image tag is missing in ${url.url}") }
        val changelog = requireNotNull(url.queryParameters[LiquibasePostgresContainer.CHANGELOG_PARAM]) { "URL parameter ${LiquibasePostgresContainer.CHANGELOG_PARAM} is missing in ${url.url}" }
        return LiquibasePostgresContainer(tag, File(changelog))
    }
}
