package org.dema.graphql.dgs.autoconfigure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.netflix.graphql.dgs.internal.Jackson3DgsJsonMapper
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.NotFoundError
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurationPackage
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.KotlinModule

private const val NOT_FOUND_JSON =
    """{"__typename":"NotFoundError","message":"Reactor core missing","entityId":"e-8842","entityType":"ReactorCore"}"""

private const val QUOTA_JSON =
    """{"__typename":"QuotaExceededError","message":"Dilithium quota spent","limit":7}"""

data class QuotaExceededError(
    override val message: String,
    val limit: Int,
) : ErrorInterface

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
    fun `the untouched dgs json mapper resolves a built-in error from its typename`() {
        val mapper = Jackson3DgsJsonMapper()
        assertThat(mapper.readValue(NOT_FOUND_JSON, ErrorInterface::class.java), "dgs own mapper cannot resolve __typename unaided")
            .isEqualTo(NotFoundError(message = "Reactor core missing", entityId = "e-8842", entityType = "ReactorCore"))
    }

    @Test
    fun `a consumer defined error resolves once the scan has registered it`() {
        runner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.readValue(QUOTA_JSON, ErrorInterface::class.java), "consumer defined subtype stays unresolved after the scan")
                .isEqualTo(QuotaExceededError(message = "Dilithium quota spent", limit = 7))
        }
    }

    @AutoConfigurationPackage
    @Configuration
    open class StarterPackagesOnly {
        @Bean
        open fun kotlinModule(): KotlinModule = KotlinModule.Builder().build()
    }
}
