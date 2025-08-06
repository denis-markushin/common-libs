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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * GraphQL scalar for handling Java [LocalDateTime] values in ISO-8601 format.
 */
@DgsScalar(name = "LocalDateTime")
class LocalDateTimeScalar : Coercing<LocalDateTime, String> {
    /**
     * Serializes the [dataFetcherResult] into an ISO date-time string.
     *
     * @throws CoercingSerializeException if [dataFetcherResult] is not a [LocalDateTime].
     */
    override fun serialize(
        dataFetcherResult: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): String = (dataFetcherResult as LocalDateTime)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ?: throw CoercingSerializeException("Expected LocalDateTime, but got: ${dataFetcherResult::class}")

    /**
     * Parses a [LocalDateTime] value provided by the client.
     *
     * @throws CoercingParseValueException if the value cannot be parsed.
     */
    override fun parseValue(
        input: Any,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime = try {
            LocalDateTime.parse(input.toString(), DateTimeFormatter.ISO_DATE_TIME)
        } catch (_: Exception) {
            throw CoercingParseValueException("Invalid LocalDateTime input: $input")
        }

    /**
     * Parses a literal [LocalDateTime] value from the GraphQL AST.
     *
     * @throws CoercingParseLiteralException if the literal is invalid.
     */
    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale,
    ): LocalDateTime {
        if (input is StringValue) {
            return try {
                LocalDateTime.parse(input.value, DateTimeFormatter.ISO_DATE_TIME)
            } catch (_: Exception) {
                throw CoercingParseLiteralException("Invalid ISO date time: ${input.value}")
            }
        }
        throw CoercingParseLiteralException("Expected AST type 'StringValue' but was: ${input::class}")
    }
}
