package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.LocalDateTime
import java.util.TimeZone

class DemaJacksonAutoConfigurationTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                DemaJacksonAutoConfiguration::class.java,
                JacksonAutoConfiguration::class.java,
            )
        )

    @Test
    fun `FAIL_ON_NULL_FOR_PRIMITIVES is enabled`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)).isTrue()
        }
    }

    @Test
    fun `READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE is enabled`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.isEnabled(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)).isTrue()
        }
    }

    @Test
    fun `FAIL_ON_UNKNOWN_PROPERTIES is disabled`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse()
        }
    }

    @Test
    fun `WRITE_DATES_AS_TIMESTAMPS is disabled`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse()
        }
    }

    @Test
    fun `WRITE_DURATIONS_AS_TIMESTAMPS is disabled`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.isEnabled(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)).isFalse()
        }
    }

    @Test
    fun `serialization inclusion is NON_NULL`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.serializationConfig.defaultPropertyInclusion.valueInclusion)
                .isEqualTo(JsonInclude.Include.NON_NULL)
        }
    }

    @Test
    fun `default timezone is UTC`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            assertThat(mapper.serializationConfig.timeZone)
                .isEqualTo(TimeZone.getTimeZone("UTC"))
        }
    }

    @Test
    fun `LocalDateTime serializes as ISO string not timestamp`() {
        contextRunner.run { ctx ->
            val mapper = ctx.getBean(ObjectMapper::class.java)
            val result = mapper.writeValueAsString(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
            assertThat(result).isEqualTo("\"2024-01-15T10:30:00\"")
        }
    }
}
