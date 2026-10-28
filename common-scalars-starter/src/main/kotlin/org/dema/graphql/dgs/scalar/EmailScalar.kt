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
 * GraphQL scalar for an e-mail address: one `@`, no whitespace, a dot in the domain,
 * at most 254 characters. Surrounding whitespace is trimmed, case is kept.
 */
@DgsScalar(name = "Email")
class EmailScalar : Coercing<String, String> {
    private val pattern = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String =
        email(dataFetcherResult) ?: throw CoercingSerializeException("Invalid Email output: '$dataFetcherResult'")

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): String =
        email(input) ?: throw CoercingParseValueException("Invalid Email input: '$input'")

    override fun parseLiteral(input: Value<*>, variables: CoercedVariables, graphQLContext: GraphQLContext, locale: Locale): String =
        (input as? StringValue)?.value?.let(::email)
            ?: throw CoercingParseLiteralException("Invalid Email literal: '$input'")

    private fun email(value: Any): String? =
        (value as? String)?.trim()?.takeIf { it.length <= 254 && pattern.matches(it) }
}
