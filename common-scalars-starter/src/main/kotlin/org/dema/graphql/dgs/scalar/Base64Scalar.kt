package org.dema.graphql.dgs.scalar

import com.netflix.graphql.dgs.DgsScalar
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.util.Base64
import java.util.Locale

/**
 * GraphQL scalar that serializes and deserializes Base64 encoded strings.
 */
@DgsScalar(name = "Base64")
class Base64Scalar : Coercing<String, String> {
    private val pattern: Regex = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$".toRegex()

    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String =
        if (dataFetcherResult is String) {
            Base64.getEncoder().encodeToString(dataFetcherResult.toByteArray(Charsets.UTF_8))
        } else {
            throw CoercingSerializeException("$dataFetcherResult is not Base64")
        }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): String =
        if (input is String) {
            validate(input)
            String(Base64.getDecoder().decode(input))
        } else {
            throw CoercingParseValueException("Expected ${String::class} but found ${input::class}")
        }

    override fun parseLiteral(input: Value<*>, variables: CoercedVariables, graphQLContext: GraphQLContext, locale: Locale): String =
        if (input is StringValue) {
            validate(input.value)
            String(Base64.getDecoder().decode(input.value))
        } else {
            throw CoercingParseLiteralException("Value '$input' has incorrect format")
        }

    private fun validate(input: String) {
        if (!pattern.matches(input)) throw CoercingSerializeException("$input is not valid Base64 string")
    }
}
