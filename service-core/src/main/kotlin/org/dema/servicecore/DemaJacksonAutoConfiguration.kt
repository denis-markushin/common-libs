package org.dema.servicecore

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean
import java.time.ZoneOffset
import java.util.TimeZone

@AutoConfigureBefore(JacksonAutoConfiguration::class)
@AutoConfiguration
class DemaJacksonAutoConfiguration {

    @Bean
    fun jacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer =
        Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder
                .featuresToEnable(
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                    DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE,
                )
                .featuresToDisable(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                    SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS,
                )
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .modules(JavaTimeModule(), KotlinModule.Builder().build())
                .timeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
        }
}
