package org.dema.outbox

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class OutboxLiquibaseMigratorIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val pg = PostgreSQLContainer("postgres:17.5-alpine")
    }

    @Test
    fun `migrator creates outbox_events table and index`() {
        val ds = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)
        OutboxLiquibaseMigrator(ds).afterPropertiesSet()

        val jdbc = JdbcTemplate(ds)
        val table = jdbc.queryForObject(
            "select count(*) from information_schema.tables where table_name = 'outbox_events'",
            Int::class.java,
        )
        val index = jdbc.queryForObject(
            "select count(*) from pg_indexes where indexname = 'idx_outbox_unpublished'",
            Int::class.java,
        )
        assertThat(table).isEqualTo(1)
        assertThat(index).isEqualTo(1)
    }
}
