package org.dema.graphql.dgs.scalar

import com.netflix.graphql.dgs.DgsScalar
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.dema.graphql.dgs.LocalDateTimeScalar
import org.dema.graphql.dgs.utils.DateUtils
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * GraphQL scalar for Java [LocalDateTime] values that hold UTC time: on the wire they carry
 * a trailing `Z`, such as `2026-11-24T10:15:30.5Z`, while the value itself is never shifted.
 * Default `LocalDateTime` scalar unless `dgs.graphql.dema.scalars.localdatetime.format` says otherwise.
 */
@DgsScalar(name = "LocalDateTime")
class ZuluLocalDateTimeScalar : LocalDateTimeScalar {

    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String = (dataFetcherResult as? LocalDateTime)
        ?.format(DateUtils.ZULU_FORMATTER)
        ?: throw CoercingSerializeException("Expected a LocalDateTime, but got: '${dataFetcherResult::class}'")

    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime = try {
        LocalDateTime.parse(input.toString(), DateUtils.ZULU_FORMATTER)
    } catch (exc: DateTimeParseException) {
        throw CoercingParseValueException("Invalid input: '$input'", exc)
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime {
        if (input !is StringValue) {
            throw CoercingParseLiteralException("Expected AST type 'StringValue' but was: ${input::class}")
        }
        return try {
            LocalDateTime.parse(input.value, DateUtils.ZULU_FORMATTER)
        } catch (exc: DateTimeParseException) {
            throw CoercingParseLiteralException("Invalid input: '${input.value}'", exc)
        }
    }

    override fun valueToLiteral(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): Value<*> = StringValue.of(serialize(input, graphQLContext, locale))
}
