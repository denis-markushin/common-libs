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
 * GraphQL scalar for binary data: a Base64 string on the wire, raw bytes on the JVM,
 * so payloads that are not text, such as PNG or PDF, pass unchanged.
 */
@DgsScalar(name = "Base64")
class Base64Scalar : Coercing<ByteArray, String> {
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String =
        (dataFetcherResult as? ByteArray)?.let(Base64.getEncoder()::encodeToString)
            ?: throw CoercingSerializeException("Expected ByteArray for Base64 output but found ${dataFetcherResult::class}")

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): ByteArray =
        decode(input as? String ?: throw CoercingParseValueException("Expected String for Base64 input but found ${input::class}")) {
            CoercingParseValueException("Invalid Base64 input: ${it.message}", it)
        }

    override fun parseLiteral(input: Value<*>, variables: CoercedVariables, graphQLContext: GraphQLContext, locale: Locale): ByteArray =
        decode((input as? StringValue)?.value ?: throw CoercingParseLiteralException("Expected StringValue for Base64 literal but found ${input::class}")) {
            CoercingParseLiteralException("Invalid Base64 literal: ${it.message}", it)
        }

    private fun decode(value: String, error: (IllegalArgumentException) -> Exception): ByteArray =
        try {
            Base64.getDecoder().decode(value)
        } catch (exc: IllegalArgumentException) {
            throw error(exc)
        }
}
