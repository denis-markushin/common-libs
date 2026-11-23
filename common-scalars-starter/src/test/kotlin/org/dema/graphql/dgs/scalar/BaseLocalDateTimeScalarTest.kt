package org.dema.graphql.dgs.scalar

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.schema.CoercingSerializeException
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Locale
import kotlin.random.Random

class BaseLocalDateTimeScalarTest {
    @Test
    fun `serialize rejects a value that is not a LocalDateTime`() {
        assertThat(
            runCatching { BaseLocalDateTimeScalar().serialize(Random.nextLong(), GraphQLContext.getDefault(), Locale.ROOT) },
            "LocalDateTime let a foreign type into the response",
        ).isFailure().isInstanceOf(CoercingSerializeException::class)
    }

    @Test
    fun `valueToLiteral writes a moment that parseLiteral reads back`() {
        val moment = moment()
        assertThat(
            BaseLocalDateTimeScalar().parseLiteral(
                BaseLocalDateTimeScalar().valueToLiteral(moment, GraphQLContext.getDefault(), Locale.ROOT),
                CoercedVariables.emptyVariables(),
                GraphQLContext.getDefault(),
                Locale.ROOT,
            ),
            "LocalDateTime lost the moment on the way into a schema literal",
        ).isEqualTo(moment)
    }

    @Test
    fun `valueToLiteral rejects a value that is not a LocalDateTime`() {
        assertThat(
            runCatching { BaseLocalDateTimeScalar().valueToLiteral(Random.nextDouble(), GraphQLContext.getDefault(), Locale.ROOT) },
            "LocalDateTime let a foreign type into a schema literal",
        ).isFailure().isInstanceOf(CoercingSerializeException::class)
    }
}

private fun moment(): LocalDateTime =
    LocalDateTime.of(
        Random.nextInt(1, 10000),
        Random.nextInt(1, 13),
        Random.nextInt(1, 29),
        Random.nextInt(24),
        Random.nextInt(60),
        Random.nextInt(60),
        Random.nextInt(1_000_000_000),
    )
