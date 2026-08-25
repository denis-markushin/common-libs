package org.dema.graphql.dgs.error

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.annotation.JsonTypeIdResolver

/**
 * Open marker interface for typed mutation errors surfaced through GraphQL.
 *
 * Consumers may add their own subtypes by implementing this interface in their schema
 * (`implements ErrorInterface`) and providing a matching Kotlin data class plus a
 * [org.dema.graphql.dgs.error.mapper.GraphQLErrorMapper] bean. Concrete subtypes — both starter
 * built-ins and consumer additions — are collected into [ErrorSubtypes], the latter through a
 * classpath scan of all Spring Boot auto-configuration packages performed by
 * [org.dema.graphql.dgs.autoconfigure.ErrorInterfaceJacksonAutoConfiguration].
 *
 * Polymorphic JSON deserialization is wired through the GraphQL `__typename` meta-field, and the
 * mapping from that field to a subtype is declared here rather than configured on a mapper. Every
 * Jackson mapper therefore resolves a typed error payload back into the correct subtype: the
 * application's own mapper, the mapper DGS uses to back
 * `DgsQueryExecutor.executeAndExtractJsonPathAsObject`, and any mapper a consumer builds by hand.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "__typename", visible = false)
@JsonTypeIdResolver(ErrorInterfaceTypeIdResolver::class)
interface ErrorInterface {
    val message: String
}
