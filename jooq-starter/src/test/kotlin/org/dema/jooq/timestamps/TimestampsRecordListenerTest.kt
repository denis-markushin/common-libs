package org.dema.jooq.timestamps

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.Field
import org.jooq.RecordContext
import org.jooq.UpdatableRecord
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

class TimestampsRecordListenerTest {

    private val fixedInstant = Instant.parse("2026-05-31T22:00:00Z")
    private val fixedClock: Clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val expectedNow: LocalDateTime = LocalDateTime.now(fixedClock)

    private val listener = TimestampsRecordListener(TimestampsSupport(fixedClock, TimestampsProperties()))

    @Test
    fun `storeStart sets createdAt to clock now when field is null`() {
        val record = mockk<UpdatableRecord<*>>(relaxed = true)
        val createdField = mockField("created_at")
        val updatedField = mockField("updated_at")
        every { record.field("created_at") } returns createdField
        every { record.field("updated_at") } returns updatedField
        every { record.get(createdField) } returns null
        val ctx = mockk<RecordContext> { every { record() } returns record }

        listener.storeStart(ctx)

        verify { record.set(createdField, expectedNow) }
    }

    @Test
    fun `storeStart does not overwrite createdAt when field already has value`() {
        val record = mockk<UpdatableRecord<*>>(relaxed = true)
        val createdField = mockField("created_at")
        val updatedField = mockField("updated_at")
        val existing = LocalDateTime.of(2020, 1, 1, 0, 0)
        every { record.field("created_at") } returns createdField
        every { record.field("updated_at") } returns updatedField
        every { record.get(createdField) } returns existing
        val ctx = mockk<RecordContext> { every { record() } returns record }

        listener.storeStart(ctx)

        verify(exactly = 0) { record.set(createdField, any<LocalDateTime>()) }
    }

    @Test
    fun `storeStart sets updatedAt unconditionally on new record`() {
        val record = mockk<UpdatableRecord<*>>(relaxed = true)
        val createdField = mockField("created_at")
        val updatedField = mockField("updated_at")
        every { record.field("created_at") } returns createdField
        every { record.field("updated_at") } returns updatedField
        every { record.get(createdField) } returns null
        val ctx = mockk<RecordContext> { every { record() } returns record }

        listener.storeStart(ctx)

        verify { record.set(updatedField, expectedNow) }
    }

    @Test
    fun `storeStart sets updatedAt to clock now overwriting previous value on existing record`() {
        val record = mockk<UpdatableRecord<*>>(relaxed = true)
        val createdField = mockField("created_at")
        val updatedField = mockField("updated_at")
        val existingCreated = LocalDateTime.of(2020, 1, 1, 0, 0)
        every { record.field("created_at") } returns createdField
        every { record.field("updated_at") } returns updatedField
        every { record.get(createdField) } returns existingCreated
        val ctx = mockk<RecordContext> { every { record() } returns record }

        listener.storeStart(ctx)

        verify { record.set(updatedField, expectedNow) }
    }

    @Test
    fun `storeStart skips column when type is not LocalDateTime`() {
        val record = mockk<UpdatableRecord<*>>(relaxed = true)
        val createdField = mockk<Field<String>>()
        every { createdField.type } returns String::class.java
        every { record.field("created_at") } returns createdField
        every { record.field("updated_at") } returns null
        val ctx = mockk<RecordContext> { every { record() } returns record }

        listener.storeStart(ctx)

        verify(exactly = 0) { record.set(any<Field<LocalDateTime>>(), any<LocalDateTime>()) }
    }

    @Test
    fun `storeStart skips column when field is absent from table`() {
        val record = mockk<UpdatableRecord<*>>(relaxed = true)
        every { record.field("created_at") } returns null
        every { record.field("updated_at") } returns null
        val ctx = mockk<RecordContext> { every { record() } returns record }

        listener.storeStart(ctx)

        verify(exactly = 0) { record.set(any<Field<LocalDateTime>>(), any<LocalDateTime>()) }
    }

    @Test
    fun `listener uses configured custom column names`() {
        val customListener = TimestampsRecordListener(
            TimestampsSupport(fixedClock, TimestampsProperties(createdAtColumn = "tstamp_created", updatedAtColumn = "tstamp_modified")),
        )
        val record = mockk<UpdatableRecord<*>>(relaxed = true)
        val createdField = mockField("tstamp_created")
        val updatedField = mockField("tstamp_modified")
        every { record.field("tstamp_created") } returns createdField
        every { record.field("tstamp_modified") } returns updatedField
        every { record.get(createdField) } returns null
        val ctx = mockk<RecordContext> { every { record() } returns record }

        customListener.storeStart(ctx)

        verify { record.set(updatedField, expectedNow) }
    }

    @Test
    fun `storeStart populates offsetdatetime updated column`() {
        val record = DSL.using(org.jooq.SQLDialect.POSTGRES).newRecord(
            field(name("updated_at"), SQLDataType.OFFSETDATETIME),
        )
        val ctx = mockk<RecordContext> { every { record() } returns record }

        listener.storeStart(ctx)

        assertThat(record.get("updated_at", OffsetDateTime::class.java))
            .isEqualTo(OffsetDateTime.now(fixedClock))
    }

    private fun mockField(name: String): Field<LocalDateTime> {
        val field = mockk<Field<LocalDateTime>>()
        every { field.type } returns LocalDateTime::class.java
        every { field.name } returns name
        return field
    }
}
