package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.dema.graphql.dgs.error.ValidationError
import org.dema.graphql.dgs.error.exception.DomainValidationException
import org.dema.graphql.dgs.error.mapper.ValidationErrorMapper
import org.junit.jupiter.api.Test

class ValidationErrorMapperTest {
    private val mapper = ValidationErrorMapper()

    @Test
    fun `ORDER constant is 300`() {
        assertThat(ValidationErrorMapper.ORDER).isEqualTo(300)
        assertThat(mapper.order).isEqualTo(300)
    }

    @Test
    fun `DomainValidationException maps path and value`() {
        val e = DomainValidationException(message = "bad", path = "input.title", value = 42)
        val result = mapper.map(e)
        assertThat(result).isEqualTo(
            ValidationError(message = "bad", path = "input.title", value = "42"),
        )
    }

    @Test
    fun `unrelated exception returns null`() {
        assertThat(mapper.map(RuntimeException("nope"))).isNull()
    }

    @Test
    fun `bare IllegalArgumentException returns null after legacy branch removed`() {
        assertThat(mapper.map(IllegalArgumentException("legacy"))).isNull()
    }
}
