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
import java.util.Locale

/**
 * GraphQL scalar for a phone number in E.164: `+` and 8 to 15 digits, the first one not zero.
 * Spaces, dashes, dots and parentheses are dropped, so the value is always canonical.
 */
@DgsScalar(name = "Phone")
class PhoneScalar : Coercing<String, String> {
    private val separator = Regex("[\\s\\-().]")

    private val pattern = Regex("\\+[1-9]\\d{7,14}")

    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String =
        phone(dataFetcherResult) ?: throw CoercingSerializeException("Invalid Phone output: '$dataFetcherResult'")

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): String =
        phone(input) ?: throw CoercingParseValueException("Invalid Phone input: '$input'")

    override fun parseLiteral(input: Value<*>, variables: CoercedVariables, graphQLContext: GraphQLContext, locale: Locale): String =
        (input as? StringValue)?.value?.let(::phone)
            ?: throw CoercingParseLiteralException("Invalid Phone literal: '$input'")

    private fun phone(value: Any): String? =
        (value as? String)?.replace(separator, "")?.takeIf(pattern::matches)
}
