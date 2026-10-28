package org.dema.graphql.dgs.scalar

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.IntValue
import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.Locale
import kotlin.random.Random

class EmailScalarTest {
    @Test
    fun `parseValue keeps a valid address as is`() {
        val email = "${word(8)}.${word(3)}@${word(6)}.${word(2)}"
        assertThat(
            EmailScalar().parseValue(email, GraphQLContext.getDefault(), Locale.ROOT),
            "Email changed a valid address",
        ).isEqualTo(email)
    }

    @Test
    fun `parseValue trims surrounding whitespace`() {
        val email = "${word(5)}@${word(7)}.org"
        assertThat(
            EmailScalar().parseValue(" \t$email  ", GraphQLContext.getDefault(), Locale.ROOT),
            "Email kept whitespace around the address",
        ).isEqualTo(email)
    }

    @Test
    fun `parseValue rejects an address without at sign`() {
        assertFailure { EmailScalar().parseValue("${word(6)}.${word(5)}.ru", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects an address with two at signs`() {
        assertFailure { EmailScalar().parseValue("${word(4)}@${word(4)}@${word(5)}.ru", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects a domain without dot`() {
        assertFailure { EmailScalar().parseValue("${word(6)}@${word(9)}", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects whitespace inside the address`() {
        assertFailure { EmailScalar().parseValue("${word(3)} ${word(4)}@${word(5)}.com", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects an address longer than 254 characters`() {
        assertFailure { EmailScalar().parseValue("${word(250)}@${word(3)}.ru", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects a non string input`() {
        assertFailure { EmailScalar().parseValue(Random.nextLong(), GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseLiteral trims a string literal`() {
        val email = "${word(7)}@${word(4)}.net"
        assertThat(
            EmailScalar().parseLiteral(
                StringValue.of("  $email "),
                CoercedVariables.emptyVariables(),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            ),
            "Email literal kept whitespace around the address",
        ).isEqualTo(email)
    }

    @Test
    fun `parseLiteral rejects a non string literal`() {
        assertFailure {
            EmailScalar().parseLiteral(
                IntValue(BigInteger.valueOf(Random.nextLong())),
                CoercedVariables.emptyVariables(),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            )
        }.isInstanceOf(CoercingParseLiteralException::class)
    }

    @Test
    fun `serialize rejects a stored value that is not an address`() {
        assertFailure { EmailScalar().serialize(word(12), GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingSerializeException::class)
    }
}

private fun word(length: Int): String = (1..length).map { ('a'..'z').random() }.joinToString("")
