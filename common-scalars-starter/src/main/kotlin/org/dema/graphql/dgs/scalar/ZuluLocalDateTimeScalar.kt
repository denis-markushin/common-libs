package org.dema.graphql.dgs.scalar

import com.netflix.graphql.dgs.DgsScalar
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingSerializeException
import org.dema.graphql.dgs.LocalDateTimeScalar
import org.dema.graphql.dgs.utils.DateUtils
import java.time.LocalDateTime
import java.util.Locale

@DgsScalar(name = "LocalDateTime")
class ZuluLocalDateTimeScalar : LocalDateTimeScalar {

    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String = (dataFetcherResult as LocalDateTime)
        .format(DateUtils.ZULU_FORMATTER)
        ?: throw CoercingSerializeException("Expected a LocalDateTime, but got: '${dataFetcherResult::class}'")

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime = try {
        LocalDateTime.parse(input.toString(), DateUtils.ZULU_FORMATTER)
    } catch (exc: Exception) {
        throw CoercingParseLiteralException("Invalid input: '$input'", exc)
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime {
        if (input is StringValue) {
            try {
                LocalDateTime.parse(input.value, DateUtils.ZULU_FORMATTER)
            } catch (exc: Exception) {
                throw CoercingParseLiteralException("Invalid input: '$input'", exc)
            }
        }
        throw CoercingParseLiteralException("Expected AST type 'StringValue' but was: ${input::class}")
    }
}
