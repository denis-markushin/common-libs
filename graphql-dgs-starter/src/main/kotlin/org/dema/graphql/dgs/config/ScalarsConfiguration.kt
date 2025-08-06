package org.dema.graphql.dgs.config

import org.dema.graphql.dgs.scalar.Base64Scalar
import org.dema.graphql.dgs.scalar.LocalDateTimeScalar
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ScalarsConfiguration {
    @Bean
    fun localDateTimeScalar(): LocalDateTimeScalar = LocalDateTimeScalar()

    @Bean
    fun base64Scalar(): Base64Scalar = Base64Scalar()
}
