package org.dema.graphql.dgs.config

import org.dema.graphql.dgs.LocalDateTimeScalar
import org.dema.graphql.dgs.scalar.Base64Scalar
import org.dema.graphql.dgs.scalar.BaseLocalDateTimeScalar
import org.dema.graphql.dgs.scalar.ZuluLocalDateTimeScalar
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ScalarsConfiguration {

    @Bean
    @ConditionalOnProperty("date.zulu.format.enabled", havingValue = "true")
    fun zuluLocalDateTimeScalar(): ZuluLocalDateTimeScalar = ZuluLocalDateTimeScalar()

    @Bean
    @ConditionalOnMissingBean(value = [LocalDateTimeScalar::class])
    fun localDateTimeScalar(): BaseLocalDateTimeScalar = BaseLocalDateTimeScalar()

    @Bean
    fun base64Scalar(): Base64Scalar = Base64Scalar()
}
