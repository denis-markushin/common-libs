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
@ConditionalOnProperty(prefix = "dgs.graphql.dema.scalars", name = ["enabled"], havingValue = "true")
class ScalarsConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "dgs.graphql.dema.scalars.localdatetime", name = ["format"], havingValue = "zulu")
    fun zuluLocalDateTimeScalar(): ZuluLocalDateTimeScalar = ZuluLocalDateTimeScalar()

    @Bean
    @ConditionalOnMissingBean(value = [LocalDateTimeScalar::class])
    fun localDateTimeScalar(): BaseLocalDateTimeScalar = BaseLocalDateTimeScalar()

    @Bean
    fun base64Scalar(): Base64Scalar = Base64Scalar()
}
