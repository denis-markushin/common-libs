package org.dema.graphql.dgs.error

import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.databind.DatabindContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase

/**
 * Resolves the GraphQL `__typename` discriminator to a concrete [ErrorInterface] subtype.
 *
 * Declared on the interface itself rather than configured per mapper, so every Jackson mapper
 * honours it: the application mapper, the mapper DGS uses to extract query results, and any
 * mapper a consumer assembles by hand.
 */
class ErrorInterfaceTypeIdResolver : TypeIdResolverBase() {

    override fun idFromValue(context: DatabindContext, value: Any): String = value.javaClass.simpleName

    override fun idFromValueAndType(context: DatabindContext, value: Any?, suggestedType: Class<*>): String =
        suggestedType.simpleName

    override fun typeFromId(context: DatabindContext, id: String): JavaType? =
        ErrorSubtypes.resolve(id)?.let { context.constructType(it) }

    override fun getDescForKnownTypeIds(): String = ErrorSubtypes.typenames().sorted().joinToString(", ")

    override fun getMechanism(): JsonTypeInfo.Id = JsonTypeInfo.Id.CUSTOM
}
