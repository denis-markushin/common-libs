package org.dema.test.actuator

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test
import org.springframework.boot.actuate.endpoint.ExposableEndpoint
import org.springframework.boot.actuate.endpoint.web.PathMappedEndpoints
import org.springframework.context.ApplicationContext

/**
 * Actuator endpoints a service exposes over the web, read from the endpoints the actuator actually
 * mapped. Under security-starter an anonymous caller reaches only health and prometheus, while any
 * other endpoint asks for nothing beyond authentication: exposure is all that keeps a heap dump or
 * the environment away from the holder of any token.
 */
interface ActuatorExposure {
    @Test
    fun `actuator exposes nothing beyond health and prometheus`(context: ApplicationContext) {
        val exposed = context.getBean(PathMappedEndpoints::class.java).map { (it as ExposableEndpoint<*>).endpointId.toString() }
        assertThat(exposed - setOf("health", "prometheus"), "actuator exposes endpoints beyond health and prometheus over the web").isEmpty()
    }
}
