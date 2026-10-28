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

class PhoneScalarTest {
    @Test
    fun `parseValue drops spaces dashes and parentheses`() {
        val code = digits(3)
        val number = digits(7)
        assertThat(
            PhoneScalar().parseValue(
                "+7 ($code) ${number.take(3)}-${number.substring(3, 5)}-${number.takeLast(2)}",
                GraphQLContext.getDefault(),
                Locale.ROOT,
            ),
            "Phone kept separators in the number",
        ).isEqualTo("+7$code$number")
    }

    @Test
    fun `parseValue drops dots between digit groups`() {
        val number = digits(10)
        assertThat(
            PhoneScalar().parseValue("+49.${number.take(4)}.${number.drop(4)}", GraphQLContext.getDefault(), Locale.ROOT),
            "Phone kept dots in the number",
        ).isEqualTo("+49$number")
    }

    @Test
    fun `parseValue keeps a canonical number as is`() {
        val phone = "+${Random.nextInt(1, 10)}${digits(Random.nextInt(7, 15))}"
        assertThat(
            PhoneScalar().parseValue(phone, GraphQLContext.getDefault(), Locale.ROOT),
            "Phone changed a canonical number",
        ).isEqualTo(phone)
    }

    @Test
    fun `parseValue rejects a number without plus`() {
        assertFailure { PhoneScalar().parseValue("8${digits(10)}", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects a country code starting with zero`() {
        assertFailure { PhoneScalar().parseValue("+0${digits(10)}", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects fewer than eight digits`() {
        assertFailure { PhoneScalar().parseValue("+${Random.nextInt(1, 10)}${digits(6)}", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects more than fifteen digits`() {
        assertFailure { PhoneScalar().parseValue("+${Random.nextInt(1, 10)}${digits(15)}", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects letters in the number`() {
        assertFailure { PhoneScalar().parseValue("+7${digits(4)}abc${digits(3)}", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseValue rejects a non string input`() {
        assertFailure { PhoneScalar().parseValue(Random.nextLong(1, Long.MAX_VALUE), GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseLiteral normalizes a string literal`() {
        val number = digits(10)
        assertThat(
            PhoneScalar().parseLiteral(
                StringValue.of("+7 ${number.take(3)} ${number.drop(3)}"),
                CoercedVariables.emptyVariables(),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            ),
            "Phone literal kept separators in the number",
        ).isEqualTo("+7$number")
    }

    @Test
    fun `parseLiteral rejects a non string literal`() {
        assertFailure {
            PhoneScalar().parseLiteral(
                IntValue(BigInteger(digits(11))),
                CoercedVariables.emptyVariables(),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            )
        }.isInstanceOf(CoercingParseLiteralException::class)
    }

    @Test
    fun `serialize normalizes a stored number`() {
        val number = digits(10)
        assertThat(
            PhoneScalar().serialize("+7-${number.take(3)}-${number.drop(3)}", GraphQLContext.getDefault(), Locale.ROOT),
            "Phone output kept separators in the number",
        ).isEqualTo("+7$number")
    }

    @Test
    fun `serialize rejects a stored value that is not a number`() {
        assertFailure { PhoneScalar().serialize("8 ${digits(10)}", GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingSerializeException::class)
    }
}

private fun digits(length: Int): String = (1..length).map { Random.nextInt(10) }.joinToString("")
