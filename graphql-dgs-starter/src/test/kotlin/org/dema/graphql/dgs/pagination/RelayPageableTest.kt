package org.dema.graphql.dgs.pagination

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.dema.graphql.dgs.utils.CursorUtils.encodeToBase64
import org.dema.graphql.dgs.utils.OrderByClausesMapping
import org.jooq.impl.AutoConverter
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

enum class MomentSortKey {
    MOMENT_DESC,
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
}
