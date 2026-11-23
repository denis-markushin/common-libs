package org.dema.graphql.dgs.pagination

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.dema.graphql.dgs.error.exception.DomainValidationException
import org.dema.graphql.dgs.utils.CursorUtils.encodeToBase64
import org.dema.graphql.dgs.utils.OrderByClausesMapping
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

enum class CursorSortKey {
    TEXT,
    JSON,
    SIZE,
    TYPE,
    VOID,
}

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
        assertThat(field.dataType.convert(pageable.seekValues.single()), "курсор потерял момент поля timestamptz").isEqualTo(moment)
    }

    @Test
    fun `random text in after is rejected as a client error`() {
        OrderByClausesMapping.register { key(CursorSortKey.TEXT).fields(DSL.field(DSL.name("text_${UUID.randomUUID()}"), SQLDataType.UUID).asc()) }
        assertFailure { RelayPageable(CursorSortKey.TEXT, UUID.randomUUID().toString(), 1 + Random.nextInt(50)) }
            .isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `base64 text that is not json is rejected as a client error`() {
        OrderByClausesMapping.register { key(CursorSortKey.JSON).fields(DSL.field(DSL.name("json_${UUID.randomUUID()}"), SQLDataType.UUID).asc()) }
        val after = Base64.getEncoder().encodeToString("{${UUID.randomUUID()}".toByteArray())
        assertFailure { RelayPageable(CursorSortKey.JSON, after, 1 + Random.nextInt(50)) }
            .isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `cursor with a wrong number of values is rejected as a client error`() {
        OrderByClausesMapping.register { key(CursorSortKey.SIZE).fields(DSL.field(DSL.name("size_${UUID.randomUUID()}"), SQLDataType.UUID).asc()) }
        val after = Array(Random.nextInt(2, 6)) { UUID.randomUUID().toString() }.encodeToBase64()
        assertFailure { RelayPageable(CursorSortKey.SIZE, after, 1 + Random.nextInt(50)) }
            .isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `cursor value of a wrong type is rejected as a client error`() {
        OrderByClausesMapping.register { key(CursorSortKey.TYPE).fields(DSL.field(DSL.name("type_${UUID.randomUUID()}"), SQLDataType.BIGINT).asc()) }
        val after = arrayOf(UUID.randomUUID().toString()).encodeToBase64()
        assertFailure { RelayPageable(CursorSortKey.TYPE, after, 1 + Random.nextInt(50)) }
            .isInstanceOf(DomainValidationException::class)
    }

    @Test
    fun `cursor with a null value is rejected as a client error`() {
        OrderByClausesMapping.register { key(CursorSortKey.VOID).fields(DSL.field(DSL.name("void_${UUID.randomUUID()}"), SQLDataType.LOCALDATETIME).asc()) }
        val after = arrayOf<Any?>(null).encodeToBase64()
        assertFailure { RelayPageable(CursorSortKey.VOID, after, 1 + Random.nextInt(50)) }
            .isInstanceOf(DomainValidationException::class)
    }
}
