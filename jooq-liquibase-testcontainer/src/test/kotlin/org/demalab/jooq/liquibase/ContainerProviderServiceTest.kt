package org.demalab.jooq.liquibase

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guards the service loader entry that registers the `liquibasepostgres` JDBC URL scheme.
 *
 * Testcontainers reads the file only inside a consumer build, so a misspelled class name would pass
 * this build and fail jOOQ codegen in the consumer.
 */
class ContainerProviderServiceTest {

    @Test
    fun `names only classes that exist`() {
        val orphans = File("src/main/resources/META-INF/services/org.testcontainers.containers.JdbcDatabaseContainerProvider")
            .readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .filter { runCatching { Class.forName(it, false, javaClass.classLoader) }.isFailure }
        assertThat(orphans, "service loader names container providers that do not exist, consumer codegen fails").isEmpty()
    }
}
