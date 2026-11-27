package org.demalab.outbox

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

/**
 * Database of one test, created in a shared container and migrated with the
 * shipped outbox changelog, so the test sees only its own rows and leaves
 * nothing for the next one to clean up.
 */
internal open class Outbox(
    private val container: PostgreSQLContainer,
) {
    private val url = container.database()
    val source = session()
    val jdbc = JdbcTemplate(source)
    val store = OutboxStore(jdbc)
    val tx = TransactionTemplate(DataSourceTransactionManager(source))

    init {
        OutboxLiquibaseMigrator(source).afterPropertiesSet()
    }

    fun session(): DriverManagerDataSource = DriverManagerDataSource(url, container.username, container.password)

    fun connection(): Connection = DriverManager.getConnection(url, container.username, container.password)
}

/** Creates an empty database of its own in the container and returns its JDBC URL. */
internal fun PostgreSQLContainer.database(): String {
    val name = "outbox_${UUID.randomUUID().toString().replace("-", "")}"
    JdbcTemplate(DriverManagerDataSource(jdbcUrl, username, password)).execute("create database $name")
    return "jdbc:postgresql://$host:${getMappedPort(5432)}/$name"
}
