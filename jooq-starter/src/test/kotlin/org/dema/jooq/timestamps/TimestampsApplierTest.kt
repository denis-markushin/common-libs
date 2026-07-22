package org.dema.jooq.timestamps

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name
import org.jooq.impl.SQLDataType
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TimestampsApplierTest {

    @Test
    fun `populates offsetdatetime created and updated columns`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("created_dt"), SQLDataType.OFFSETDATETIME),
            field(name("last_edited_dt"), SQLDataType.OFFSETDATETIME),
        )
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("created_dt", OffsetDateTime::class.java))
            .isEqualTo(OffsetDateTime.now(clock))
    }

    @Test
    fun `populates offsetdatetime updated column`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("created_dt"), SQLDataType.OFFSETDATETIME),
            field(name("last_edited_dt"), SQLDataType.OFFSETDATETIME),
        )
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("last_edited_dt", OffsetDateTime::class.java))
            .isEqualTo(OffsetDateTime.now(clock))
    }

    @Test
    fun `overwrites updated column when already set`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("created_dt"), SQLDataType.OFFSETDATETIME),
            field(name("last_edited_dt"), SQLDataType.OFFSETDATETIME),
        )
        record.set(field(name("last_edited_dt"), SQLDataType.OFFSETDATETIME), OffsetDateTime.parse("2000-01-01T00:00:00Z"))
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("last_edited_dt", OffsetDateTime::class.java))
            .isEqualTo(OffsetDateTime.now(clock))
    }

    @Test
    fun `preserves non null created column`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("created_dt"), SQLDataType.OFFSETDATETIME),
            field(name("last_edited_dt"), SQLDataType.OFFSETDATETIME),
        )
        val original = OffsetDateTime.parse("2001-02-03T04:05:06Z")
        record.set(field(name("created_dt"), SQLDataType.OFFSETDATETIME), original)
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("created_dt", OffsetDateTime::class.java))
            .isEqualTo(original)
    }

    @Test
    fun `populates localdatetime updated column`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("created_dt"), SQLDataType.LOCALDATETIME),
            field(name("last_edited_dt"), SQLDataType.LOCALDATETIME),
        )
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("last_edited_dt", LocalDateTime::class.java))
            .isEqualTo(LocalDateTime.now(clock))
    }

    @Test
    fun `populates instant updated column`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("created_dt"), SQLDataType.INSTANT),
            field(name("last_edited_dt"), SQLDataType.INSTANT),
        )
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("last_edited_dt", Instant::class.java))
            .isEqualTo(Instant.now(clock))
    }

    @Test
    fun `ignores unsupported column type`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("last_edited_dt"), SQLDataType.VARCHAR),
        )
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("last_edited_dt")).isNull()
    }

    @Test
    fun `skips absent created column`() {
        val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("last_edited_dt"), SQLDataType.OFFSETDATETIME),
        )
        TimestampsApplier.apply(record, "created_dt", "last_edited_dt", clock)
        assertThat(record.get("last_edited_dt", OffsetDateTime::class.java))
            .isEqualTo(OffsetDateTime.now(clock))
    }
}
