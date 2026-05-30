package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.dema.graphql.dgs.error.ServiceUnavailableError
import org.dema.graphql.dgs.error.exception.ServiceUnavailableException
import org.dema.graphql.dgs.error.mapper.ServiceUnavailableErrorMapper
import org.junit.jupiter.api.Test

class ServiceUnavailableErrorMapperTest {
    private val mapper = ServiceUnavailableErrorMapper()

    @Test
    fun `ORDER constant is 600`() {
        assertThat(ServiceUnavailableErrorMapper.ORDER).isEqualTo(600)
        assertThat(mapper.order).isEqualTo(600)
    }

    @Test
    fun `ServiceUnavailableException maps message and retryAfterSeconds`() {
        val result = mapper.map(ServiceUnavailableException(message = "down", retryAfterSeconds = 30))
        assertThat(result).isEqualTo(ServiceUnavailableError(message = "down", retryAfterSeconds = 30))
    }

    @Test
    fun `ServiceUnavailableException without retryAfter maps with null`() {
        val result = mapper.map(ServiceUnavailableException(message = "temp"))
        assertThat(result).isEqualTo(ServiceUnavailableError(message = "temp", retryAfterSeconds = null))
    }

    @Test
    fun `ServiceUnavailableException with default message`() {
        val result = mapper.map(ServiceUnavailableException())
        assertThat(result).isEqualTo(ServiceUnavailableError(message = "Service unavailable"))
    }

    @Test
    fun `unrelated exception returns null`() {
        assertThat(mapper.map(RuntimeException("nope"))).isNull()
    }
}
