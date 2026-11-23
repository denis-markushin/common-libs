package org.dema.outbox

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

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

    @Test
    fun `migrator indexes published events for the purge`() {
        val ds = DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)
        OutboxLiquibaseMigrator(ds).afterPropertiesSet()
        assertThat(published(JdbcTemplate(ds)), "purge scans the whole outbox table").isEqualTo(1)
    }

    @Test
    fun `migrator rebuilds a published index that a failed build left invalid`() {
        val ds = database()
        val jdbc = JdbcTemplate(ds)
        OutboxLiquibaseMigrator(ds).afterPropertiesSet()
        jdbc.update("update pg_index set indisvalid = false where indexrelid = 'idx_outbox_published'::regclass")
        jdbc.update("delete from databasechangelog where id = 'outbox-events-published-index'")
        OutboxLiquibaseMigrator(ds).afterPropertiesSet()
        assertThat(published(jdbc), "purge kept an index that a failed build left unusable").isEqualTo(1)
    }

    @Test
    fun `migrator keeps a published index built before the upgrade`() {
        val ds = database()
        val jdbc = JdbcTemplate(ds)
        OutboxLiquibaseMigrator(ds).afterPropertiesSet()
        jdbc.update("delete from databasechangelog where id = 'outbox-events-published-index'")
        val index = jdbc.queryForObject("select 'idx_outbox_published'::regclass::oid::bigint", Long::class.java)
        OutboxLiquibaseMigrator(ds).afterPropertiesSet()
        assertThat(
            jdbc.queryForObject("select 'idx_outbox_published'::regclass::oid::bigint", Long::class.java),
            "upgrade rebuilt an index the operator had already built",
        ).isEqualTo(index)
    }

    private fun database(): DriverManagerDataSource {
        val name = "outbox_${UUID.randomUUID().toString().replace("-", "")}"
        JdbcTemplate(DriverManagerDataSource(pg.jdbcUrl, pg.username, pg.password)).execute("create database $name")
        return DriverManagerDataSource("jdbc:postgresql://${pg.host}:${pg.getMappedPort(5432)}/$name", pg.username, pg.password)
    }

    private fun published(jdbc: JdbcTemplate): Int? =
        jdbc.queryForObject(
            """
            select count(*) from pg_index
            where indexrelid = to_regclass('idx_outbox_published') and indisvalid
              and pg_get_indexdef(indexrelid) like '%(published_at)%WHERE (published_at IS NOT NULL)'
            """.trimIndent(),
            Int::class.java,
        )
}
