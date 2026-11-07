package org.dema.test.actuator

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.endpoint.Access
import org.springframework.boot.actuate.endpoint.EndpointId
import org.springframework.boot.actuate.endpoint.EndpointsSupplier
import org.springframework.boot.actuate.endpoint.ExposableEndpoint
import org.springframework.boot.actuate.endpoint.Operation
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoint
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints
import org.springframework.context.support.GenericApplicationContext

class ActuatorExposureTest {

    @Test
    fun `exposure rejects an endpoint beyond health and prometheus`() {
        val error = failure("health", listOf("env", "heapdump", "beans", "loggers").random())
        assertThat(error, "endpoint beyond health and prometheus slipped through").isNotNull().isInstanceOf<AssertionError>()
    }

    @Test
    fun `exposure accepts health and prometheus`() {
        assertThat(failure("health", "prometheus"), "exposure rejected the endpoints every service shows").isNull()
    }

    private fun failure(vararg ids: String): Throwable? =
        GenericApplicationContext().use { context ->
            context.beanFactory.registerSingleton("pathMappedEndpoints", PathMappedEndpoints("/actuator", EndpointsSupplier { ids.map(::Endpoint) }))
            context.refresh()
            runCatching { object : ActuatorExposure {}.`actuator exposes nothing beyond health and prometheus`(context) }.exceptionOrNull()
        }
}

/** Web endpoint known only by its id, as the actuator would expose it. */
private class Endpoint(
    private val id: String,
) : ExposableEndpoint<Operation>,
    PathMappedEndpoint {
    override fun getEndpointId(): EndpointId = EndpointId.of(id)

    override fun getDefaultAccess(): Access = Access.UNRESTRICTED

    override fun getOperations(): Collection<Operation> = emptyList()

    override fun getRootPath(): String = id
}
