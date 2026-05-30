package org.dema.graphql.dgs.error

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * Open marker interface for typed mutation errors surfaced through GraphQL.
 *
 * Consumers may add their own subtypes by implementing this interface in
 * their schema (`implements ErrorInterface`) and providing a matching Kotlin
 * data class plus a [org.dema.graphql.dgs.error.mapper.GraphQLErrorMapper]
 * bean. Concrete subtypes — both starter built-ins and consumer additions —
 * are discovered and registered with Jackson automatically by
 * [org.dema.graphql.dgs.autoconfigure.ErrorInterfaceJacksonAutoConfiguration]
 * via a classpath scan of all Spring Boot auto-configuration packages.
 *
 * Polymorphic JSON deserialization is wired through the GraphQL `__typename`
 * meta-field so the DGS test client (and any Jackson-based consumer) can
 * deserialize a typed error payload back into the correct subtype.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "__typename", visible = false)
interface ErrorInterface {
    val message: String
}
