package org.dema.graphql.dgs.autoconfigure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.netflix.graphql.dgs.json.DgsJsonMapper
import com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.NotFoundError
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.Configurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.KotlinModule

private const val NOT_FOUND_JSON =
    """{"__typename":"NotFoundError","message":"Reactor core missing","entityId":"e-8842","entityType":"ReactorCore"}"""

class ErrorInterfaceJacksonAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                ErrorInterfaceJacksonAutoConfiguration::class.java,
                JacksonAutoConfiguration::class.java,
            ),
        )
        .withUserConfiguration(StarterPackagesOnly::class.java)

    @Test
    fun `the boot object mapper resolves a built-in error from its typename`() {
        runner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.readValue(NOT_FOUND_JSON, ErrorInterface::class.java), "boot object mapper cannot resolve __typename")
                .isEqualTo(NotFoundError(message = "Reactor core missing", entityId = "e-8842", entityType = "ReactorCore"))
        }
    }

    @Test
    fun `the dgs json mapper resolves a built-in error from its typename`() {
        runner.run { ctx ->
            val mapper = ctx.getBean(DgsJsonMapper::class.java)
            assertThat(mapper.readValue(NOT_FOUND_JSON, ErrorInterface::class.java), "dgs json mapper cannot resolve __typename")
                .isEqualTo(NotFoundError(message = "Reactor core missing", entityId = "e-8842", entityType = "ReactorCore"))
        }
    }

    @Test
    fun `the dgs json mapper keeps unknown properties from failing extraction`() {
        runner.run { ctx ->
            val mapper = ctx.getBean(DgsJsonMapper::class.java)
            val json = """{"__typename":"NotFoundError","message":"Reactor core missing","warpFactor":9}"""
            assertThat(mapper.readValue(json, ErrorInterface::class.java), "dgs json mapper rejects unknown properties")
                .isEqualTo(NotFoundError(message = "Reactor core missing"))
        }
    }

    @Test
    fun `the starter is auto-configured ahead of the dgs json mapper default`() {
        val ordered = Configurations.getClasses(
            AutoConfigurations.of(
                DgsSpringGraphQLAutoConfiguration::class.java,
                ErrorInterfaceJacksonAutoConfiguration::class.java,
            ),
        )
        assertThat(ordered.first(), "dgs wins the json mapper bean and drops the error subtypes")
            .isEqualTo(ErrorInterfaceJacksonAutoConfiguration::class.java)
    }

    @AutoConfigurationPackage
    @Configuration
    open class StarterPackagesOnly {
        @Bean
        open fun kotlinModule(): KotlinModule = KotlinModule.Builder().build()
    }
}
