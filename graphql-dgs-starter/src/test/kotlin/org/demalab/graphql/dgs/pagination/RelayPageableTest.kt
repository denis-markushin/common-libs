package org.demalab.graphql.dgs.pagination

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.demalab.graphql.dgs.error.exception.DomainValidationException
import org.demalab.graphql.dgs.utils.CursorUtils.encodeToBase64
import org.demalab.graphql.dgs.utils.OrderByClausesMapping
import org.jooq.DataType
import org.jooq.impl.AutoConverter
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.UUID
import kotlin.random.Random

enum class MomentSortKey {
    MOMENT_DESC,
}

/**
 * Sorting keys of the broken cursor tests, one per test: the mapping registry refuses a second registration.
 */
enum class CursorSortKey {
    TEXT,
    JSON,
    SIZE,
    TYPE,
    VOID,
}

/**
 * Registers [sort] with one ascending field of [type] under a random name.
 */
private fun register(sort: CursorSortKey, type: DataType<*>) =
    OrderByClausesMapping.register { key(sort).fields(DSL.field(DSL.name("${sort.name.lowercase()}_${UUID.randomUUID()}"), type).asc()) }

class RelayPageableTest {
    @Test
    fun `seek value of converted timestamptz field binds as the cursor moment`() {
        val moment = LocalDateTime.ofEpochSecond(Random.nextLong(0, 4_000_000_000), Random.nextInt(0, 1_000_000) * 1_000, ZoneOffset.UTC)
        val field =
            DSL.field(
                DSL.name("moment_${UUID.randomUUID()}"),
                SQLDataType.TIMESTAMPWITHTIMEZONE.asConvertedDataType(AutoConverter(OffsetDateTime::class.java, LocalDateTime::class.java)),
            )
        OrderByClausesMapping.register { key(MomentSortKey.MOMENT_DESC).fields(field.desc()) }
        val pageable = RelayPageable(MomentSortKey.MOMENT_DESC, arrayOf<Any>(moment).encodeToBase64(), 1 + Random.nextInt(50))
        assertThat(field.dataType.convert(pageable.seekValues.single()), "cursor lost the moment of a timestamptz field")
            .isEqualTo(moment)
    }

    @Test
    fun `random text in after is rejected as a client error`() {
        register(CursorSortKey.TEXT, SQLDataType.UUID)
        val after = UUID.randomUUID().toString()
        val outcome = runCatching { RelayPageable(CursorSortKey.TEXT, after, 1 + Random.nextInt(50)) }
        assertThat(outcome, "random cursor text passed as a server error")
            .isFailure().isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `base64 text that is not json is rejected as a client error`() {
        register(CursorSortKey.JSON, SQLDataType.UUID)
        val after = Base64.getEncoder().encodeToString("{${UUID.randomUUID()}".toByteArray())
        val outcome = runCatching { RelayPageable(CursorSortKey.JSON, after, 1 + Random.nextInt(50)) }
        assertThat(outcome, "cursor that is not json passed as a server error")
            .isFailure().isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `cursor with a wrong number of values is rejected as a client error`() {
        register(CursorSortKey.SIZE, SQLDataType.UUID)
        val after = Array(Random.nextInt(2, 6)) { UUID.randomUUID().toString() }.encodeToBase64()
        val outcome = runCatching { RelayPageable(CursorSortKey.SIZE, after, 1 + Random.nextInt(50)) }
        assertThat(outcome, "cursor of a foreign sorting passed as a server error")
            .isFailure().isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `cursor value of a wrong type is rejected as a client error`() {
        register(CursorSortKey.TYPE, SQLDataType.BIGINT)
        val after = arrayOf(UUID.randomUUID().toString()).encodeToBase64()
        val outcome = runCatching { RelayPageable(CursorSortKey.TYPE, after, 1 + Random.nextInt(50)) }
        assertThat(outcome, "cursor value of a wrong type passed as a server error")
            .isFailure().isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `cursor with a null value is rejected as a client error`() {
        register(CursorSortKey.VOID, SQLDataType.LOCALDATETIME)
        val after = arrayOf<Any?>(null).encodeToBase64()
        val outcome = runCatching { RelayPageable(CursorSortKey.VOID, after, 1 + Random.nextInt(50)) }
        assertThat(outcome, "cursor with a null value passed as a server error")
            .isFailure().isInstanceOf(DomainValidationException::class)
    }
}
