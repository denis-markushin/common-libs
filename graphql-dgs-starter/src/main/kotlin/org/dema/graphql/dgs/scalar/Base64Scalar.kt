package org.dema.graphql.dgs.scalar

import com.netflix.graphql.dgs.DgsScalar
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import java.util.Base64

/**
 * GraphQL scalar that serializes and deserializes Base64 encoded strings.
 */
@DgsScalar(name = "Base64")
class Base64Scalar : Coercing<String, String> {
    private val pattern: Regex = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?\$".toRegex()

    /**
     * Encodes the provided [result] string into Base64.
     *
     * @throws CoercingSerializeException if [result] is not a [String].
     */
    override fun serialize(result: Any): String =
        if (result is String) {
            Base64.getEncoder().encodeToString(result.toByteArray(Charsets.UTF_8))
        } else {
            throw CoercingSerializeException("$result is not Base64")
        }

    /**
     * Decodes the provided Base64 [input] value from the client.
     *
     * @throws CoercingParseValueException if [input] is not a valid Base64 string.
     */
    override fun parseValue(input: Any): String =
        if (input is String) {
            validate(input)
            String(Base64.getDecoder().decode(input))
        } else {
            throw CoercingParseValueException("Expected ${String::class} but found ${input::class}")
        }

    /**
     * Decodes a literal Base64 value from the GraphQL query.
     *
     * @throws CoercingParseLiteralException if the literal is not a [StringValue]
     * or has invalid Base64 format.
     */
    override fun parseLiteral(input: Any): String =
        if (input is StringValue) {
            validate(input.value)
            String(Base64.getDecoder().decode(input.value))
        } else {
            throw CoercingParseLiteralException("Value '$input' has incorrect format")
        }

    /**
     * Validates that [input] conforms to the Base64 pattern.
     */
    private fun validate(input: String) {
        if (!pattern.matches(input)) throw CoercingSerializeException("$input is not valid Base64 string")
    }
}