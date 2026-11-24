package org.dema.graphql.dgs.scalar

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.random.Random

class Base64ScalarTest {
    @Test
    fun `parseValue restores bytes that are not valid UTF-8`() {
        val bytes = binary()
        assertThat(
            Base64Scalar().parseValue(
                Base64Scalar().serialize(bytes, GraphQLContext.getDefault(), Locale.ROOT),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            ).toList(),
            "Base64 corrupted binary bytes on the way through a variable",
        ).isEqualTo(bytes.toList())
    }

    @Test
    fun `parseLiteral restores bytes that are not valid UTF-8`() {
        val bytes = binary()
        assertThat(
            Base64Scalar().parseLiteral(
                StringValue.of(Base64Scalar().serialize(bytes, GraphQLContext.getDefault(), Locale.ROOT)),
                CoercedVariables.emptyVariables(),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            ).toList(),
            "Base64 corrupted binary bytes on the way through a literal",
        ).isEqualTo(bytes.toList())
    }

    @Test
    fun `parseValue rejects characters outside the Base64 alphabet`() {
        assertThat(
            runCatching { Base64Scalar().parseValue("${word(Random.nextInt(4, 40))}!*", GraphQLContext.getDefault(), Locale.ROOT) },
            "Base64 took a variable outside its alphabet",
        ).isFailure().isInstanceOf(CoercingParseValueException::class)
    }

    @Test
    fun `parseLiteral rejects characters outside the Base64 alphabet`() {
        assertThat(
            runCatching {
                Base64Scalar().parseLiteral(
                    StringValue.of("*${word(Random.nextInt(4, 40))}!"),
                    CoercedVariables.emptyVariables(),
                    GraphQLContext.getDefault(),
                    Locale.ROOT,
                )
            },
            "Base64 took a literal outside its alphabet",
        ).isFailure().isInstanceOf(CoercingParseLiteralException::class)
    }

    @Test
    fun `serialize rejects a value that is not a byte array`() {
        assertThat(
            runCatching { Base64Scalar().serialize(word(Random.nextInt(1, 30)), GraphQLContext.getDefault(), Locale.ROOT) },
            "Base64 encoded text as if it were bytes",
        ).isFailure().isInstanceOf(CoercingSerializeException::class)
    }

    @Test
    fun `valueToLiteral keeps bytes that are not valid UTF-8`() {
        val bytes = binary()
        assertThat(
            Base64Scalar().parseLiteral(
                Base64Scalar().valueToLiteral(bytes, GraphQLContext.getDefault(), Locale.ROOT),
                CoercedVariables.emptyVariables(),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            ).toList(),
            "Base64 corrupted binary bytes on the way into a schema literal",
        ).isEqualTo(bytes.toList())
    }

    @Test
    fun `valueToLiteral rejects a value that is not a byte array`() {
        assertThat(
            runCatching { Base64Scalar().valueToLiteral(word(Random.nextInt(1, 30)), GraphQLContext.getDefault(), Locale.ROOT) },
            "Base64 let text into a schema literal as if it were bytes",
        ).isFailure().isInstanceOf(CoercingSerializeException::class)
    }
}

private fun binary(): ByteArray =
    Random.nextBytes(Random.nextInt(0, 512)) + byteArrayOf(0x89.toByte(), 0x50, 0xC3.toByte(), 0x28) + Random.nextBytes(Random.nextInt(0, 512))

private fun word(length: Int): String = (1..length).map { ('a'..'z').random() }.joinToString("")
