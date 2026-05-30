package org.dema.graphql.dgs.autoconfigure

import org.dema.graphql.dgs.error.mapper.CompositeGraphQLErrorMapper
import org.dema.graphql.dgs.error.mapper.ConflictErrorMapper
import org.dema.graphql.dgs.error.mapper.ForbiddenErrorMapper
import org.dema.graphql.dgs.error.mapper.GraphQLErrorMapper
import org.dema.graphql.dgs.error.mapper.NotFoundErrorMapper
import org.dema.graphql.dgs.error.mapper.ServiceUnavailableErrorMapper
import org.dema.graphql.dgs.error.mapper.UnauthorizedErrorMapper
import org.dema.graphql.dgs.error.mapper.ValidationErrorMapper
import org.dema.graphql.dgs.mutation.DefaultMutationResolver
import org.dema.graphql.dgs.mutation.MutationResolver
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@AutoConfiguration
class GraphQLErrorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun compositeGraphQLErrorMapper(mappers: List<GraphQLErrorMapper>): CompositeGraphQLErrorMapper =
        CompositeGraphQLErrorMapper(mappers)

    // @Primary disambiguates against consumer @DgsComponent classes that use
    // `: MutationResolver by mutationResolver` and therefore also implement MutationResolver.
    // by-name @ConditionalOnMissingBean is required: a by-type check would see the consumer's
    // mutation beans as existing MutationResolvers and skip creating the default resolver entirely.
    // @ConditionalOnBean guards against partial-context test slices where CompositeGraphQLErrorMapper is absent.
    @Bean
    @Primary
    @ConditionalOnMissingBean(name = ["mutationResolver"])
    @ConditionalOnBean(CompositeGraphQLErrorMapper::class)
    fun mutationResolver(errorMapper: CompositeGraphQLErrorMapper): MutationResolver =
        DefaultMutationResolver(errorMapper)

    @Bean
    @ConditionalOnMissingBean(name = ["notFoundErrorMapper"])
    fun notFoundErrorMapper(): NotFoundErrorMapper = NotFoundErrorMapper()

    @Bean
    @ConditionalOnMissingBean(name = ["conflictErrorMapper"])
    fun conflictErrorMapper(): ConflictErrorMapper = ConflictErrorMapper()

    @Bean
    @ConditionalOnMissingBean(name = ["validationErrorMapper"])
    fun validationErrorMapper(): ValidationErrorMapper = ValidationErrorMapper()

    @Bean
    @ConditionalOnMissingBean(name = ["unauthorizedErrorMapper"])
    fun unauthorizedErrorMapper(): UnauthorizedErrorMapper = UnauthorizedErrorMapper()

    @Bean
    @ConditionalOnMissingBean(name = ["forbiddenErrorMapper"])
    fun forbiddenErrorMapper(): ForbiddenErrorMapper = ForbiddenErrorMapper()

    @Bean
    @ConditionalOnMissingBean(name = ["serviceUnavailableErrorMapper"])
    fun serviceUnavailableErrorMapper(): ServiceUnavailableErrorMapper = ServiceUnavailableErrorMapper()
}
