package org.dema.graphql.dgs.autoconfigure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.NotFoundError
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Configuration

class ErrorInterfaceJacksonAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ErrorInterfaceJacksonAutoConfiguration::class.java))
        .withUserConfiguration(StarterPackagesOnly::class.java)

    @Test
    fun `built-in NotFoundError round-trips via discriminator`() {
        runner.run { ctx ->
            val module = ctx.getBean(SimpleModule::class.java)
            val mapper = ObjectMapper().registerKotlinModule().registerModule(module)
            val json = """{"__typename":"NotFoundError","message":"Project not found","entityId":"abc","entityType":"Project"}"""
            val result = mapper.readValue(json, ErrorInterface::class.java)
            assertThat(result).isInstanceOf(NotFoundError::class)
            assertThat(result).isEqualTo(NotFoundError(message = "Project not found", entityId = "abc", entityType = "Project"))
        }
    }

    @AutoConfigurationPackage
    @Configuration
    open class StarterPackagesOnly
}
