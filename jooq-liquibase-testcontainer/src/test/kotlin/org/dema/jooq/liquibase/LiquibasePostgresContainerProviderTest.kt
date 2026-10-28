package org.dema.jooq.liquibase

import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.sql.DriverManager
import java.util.concurrent.TimeUnit

class LiquibasePostgresContainerProviderTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    fun `liquibasepostgres url hands out a connection to a migrated database`(@TempDir dir: File) {
        val changelog = File(dir, "changelog-master.yml")
        changelog.writeText(
            """
            databaseChangeLog:
              - changeSet:
                  id: create-book
                  author: test
                  changes:
                    - createTable:
                        tableName: book
                        columns:
                          - column:
                              name: id
                              type: uuid
                              constraints:
                                primaryKey: true
            """.trimIndent(),
        )
        val url = "jdbc:tc:liquibasepostgres:17.5-alpine:///test-db?changelog=${changelog.absolutePath}"
        val found = DriverManager.getConnection(url).use { it.metaData.getTables(null, "public", "book", null).next() }
        assertThat(found, "the changelog was not applied before the connection was handed out").isTrue()
    }
}
