package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import org.dema.graphql.dgs.error.mapper.ErrorSourceMapper
import org.junit.jupiter.api.Test
import java.util.UUID

class ErrorSourceMapperTest {
    @Test
    fun `exception with an error source answers its own error`() {
        val error = SeatTakenError("seat ${UUID.randomUUID()} is taken")
        assertThat(ErrorSourceMapper().map(SeatTakenException(error)), "error carried by the exception got lost")
            .isSameInstanceAs(error)
    }

    @Test
    fun `mapper returns nothing for exception without its own error`() {
        assertThat(
            ErrorSourceMapper().map(RuntimeException("seat ${UUID.randomUUID()} is taken")),
            "mapper invented an error for an exception that carries none",
        ).isNull()
    }
}
