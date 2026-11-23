package org.dema.graphql.dgs.scalar

import assertk.assertFailure
import assertk.assertions.isInstanceOf
import graphql.GraphQLContext
import graphql.schema.CoercingSerializeException
import org.junit.jupiter.api.Test
import java.util.Locale
import kotlin.random.Random

class ZuluLocalDateTimeScalarTest {
    @Test
    fun `valueToLiteral rejects a value that is not a LocalDateTime`() {
        assertFailure { ZuluLocalDateTimeScalar().valueToLiteral(Random.nextInt(), GraphQLContext.getDefault(), Locale.ROOT) }
            .isInstanceOf(CoercingSerializeException::class)
    }
}
