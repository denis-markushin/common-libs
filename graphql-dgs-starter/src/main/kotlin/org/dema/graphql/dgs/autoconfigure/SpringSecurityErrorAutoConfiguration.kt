package org.dema.graphql.dgs.autoconfigure

import org.dema.graphql.dgs.error.mapper.SpringSecurityAuthErrorMapper
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [GraphQLErrorAutoConfiguration::class])
@ConditionalOnClass(name = ["org.springframework.security.core.AuthenticationException"])
class SpringSecurityErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = ["springSecurityAuthErrorMapper"])
    fun springSecurityAuthErrorMapper(): SpringSecurityAuthErrorMapper = SpringSecurityAuthErrorMapper()
}
