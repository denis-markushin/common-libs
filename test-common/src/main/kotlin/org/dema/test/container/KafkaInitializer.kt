package org.dema.test.container

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

/**
 * Points a test context at a throwaway Kafka broker.
 *
 * The container is held statically, so one broker serves every context that names
 * this initializer instead of one per test class, and Testcontainers reaps it when
 * the JVM exits.
 */
class KafkaInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(ctx: ConfigurableApplicationContext) {
        kafka.start()
        TestPropertyValues
            .of("spring.kafka.bootstrap-servers=${kafka.bootstrapServers}")
            .applyTo(ctx.environment)
    }

    private companion object {
        val kafka = KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
    }
}
