package org.dema.jooq

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.dema.jooq.timestamps.TimestampsApplier
import org.jooq.Table
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Verifies that [buildUpsert] preserves the created column on conflict while refreshing
 * the updated column, against a live PostgreSQL instance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AbstractRepositoryUpsertIT {

    private val postgres = PostgreSQLContainer("postgres:15-alpine")
    private lateinit var dsl: org.jooq.DSLContext

    @BeforeAll
    fun setup() {
        postgres.start()
        dsl = DSL.using(postgres.jdbcUrl, postgres.username, postgres.password)
        dsl.execute(
            "create table task (id uuid primary key, payload text, created_dt timestamptz, last_edited_dt timestamptz)",
        )
    }

    @AfterAll
    fun teardown() {
        postgres.stop()
    }

    private fun taskTable(): Table<*> = dsl.meta().getTables("task").first()

    @Suppress("UNCHECKED_CAST")
    private fun upsertWithClock(table: Table<*>, id: UUID, payload: String, clock: Clock) {
        val record = dsl.newRecord(table)
        record.set(record.field("id") as org.jooq.Field<Any>, id)
        record.set(record.field("payload") as org.jooq.Field<Any>, payload)
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        buildUpsert(dsl, table, record, "created_dt").execute()
    }

    @Test
    fun `upsert preserves created on conflict`() {
        val id = UUID.randomUUID()
        val table = taskTable()
        val firstClock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        upsertWithClock(table, id, "a", firstClock)
        val created = dsl.fetchValue("select created_dt from task where id = {0}", id) as OffsetDateTime

        val secondClock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC)
        upsertWithClock(table, id, "b", secondClock)

        val createdAfter = dsl.fetchValue("select created_dt from task where id = {0}", id) as OffsetDateTime
        assertThat(createdAfter).isEqualTo(created)
    }

    @Test
    fun `upsert refreshes updated on conflict`() {
        val id = UUID.randomUUID()
        val table = taskTable()
        val firstClock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        upsertWithClock(table, id, "a", firstClock)
        val updated = dsl.fetchValue("select last_edited_dt from task where id = {0}", id) as OffsetDateTime

        val secondClock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC)
        upsertWithClock(table, id, "b", secondClock)

        val updatedAfter = dsl.fetchValue("select last_edited_dt from task where id = {0}", id) as OffsetDateTime
        assertThat(updatedAfter).isNotEqualTo(updated)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `upsert with null created column updates every non-primary-key column from excluded`() {
        val id = UUID.randomUUID()
        val table = taskTable()

        val first = dsl.newRecord(table)
        first.set(first.field("id") as org.jooq.Field<Any>, id)
        first.set(first.field("payload") as org.jooq.Field<Any>, "a")
        buildUpsert(dsl, table, first, null).execute()

        val second = dsl.newRecord(table)
        second.set(second.field("id") as org.jooq.Field<Any>, id)
        second.set(second.field("payload") as org.jooq.Field<Any>, "b")
        buildUpsert(dsl, table, second, null).execute()

        val payload = dsl.fetchValue("select payload from task where id = {0}", id) as String
        assertThat(payload).isEqualTo("b")
    }
}
