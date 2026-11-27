package org.demalab.graphql.dgs.pagination

import assertk.assertThat
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.junit.jupiter.api.Test
import java.util.UUID

class Base64EncodedCursorTest {
    @Test
    fun `cursor of a record with a null sort value cannot be built`() {
        val id = DSL.field(DSL.name("id_${UUID.randomUUID()}"), SQLDataType.UUID)
        val moment = DSL.field(DSL.name("sent_at_${UUID.randomUUID()}"), SQLDataType.LOCALDATETIME)
        val record = DSL.using(SQLDialect.POSTGRES).newRecord(moment, id).values(null, UUID.randomUUID())
        val outcome = runCatching { Base64EncodedCursor(record, arrayOf(moment, id)) }
        assertThat(outcome, "cursor took a null sort value it can never seek past")
            .isFailure().isInstanceOf(IllegalStateException::class)
    }
}
