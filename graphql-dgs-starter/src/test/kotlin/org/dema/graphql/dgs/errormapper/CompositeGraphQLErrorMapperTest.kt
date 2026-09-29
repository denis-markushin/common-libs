package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.NotFoundError
import org.dema.graphql.dgs.error.mapper.CompositeGraphQLErrorMapper
import org.dema.graphql.dgs.error.mapper.GraphQLErrorMapper
import org.junit.jupiter.api.Test
import java.util.UUID

class CompositeGraphQLErrorMapperTest {

    private val notFoundMapper = mapperOf { e ->
        if (e is IllegalStateException) NotFoundError(message = e.message ?: "nf") else null
    }
    private val nullMapper = mapperOf { null }

    @Test
    fun `first non-null mapper wins`() {
        val composite = CompositeGraphQLErrorMapper(listOf(nullMapper, notFoundMapper, nullMapper))
        val result = composite.toGraphQLError(IllegalStateException("missing"))
        assertThat(result).isInstanceOf(NotFoundError::class)
        assertThat((result as NotFoundError).message).isEqualTo("missing")
    }

    @Test
    fun `unmapped exception yields an error that hides the exception message`() {
        val detail = "Key (email)=(${UUID.randomUUID()}@mail.test) already exists"
        val composite = CompositeGraphQLErrorMapper(listOf(nullMapper))
        assertThat(composite.toGraphQLError(IllegalStateException(detail)).message).doesNotContain(detail)
    }

    @Test
    fun `unmapped exception without a message yields an error that hides the exception type`() {
        class LedgerRowConflictException : RuntimeException()
        val composite = CompositeGraphQLErrorMapper(emptyList())
        assertThat(composite.toGraphQLError(LedgerRowConflictException()).message)
            .doesNotContain("LedgerRowConflictException")
    }

    private fun mapperOf(block: (Throwable) -> ErrorInterface?): GraphQLErrorMapper =
        object : GraphQLErrorMapper {
            override fun map(e: Throwable): ErrorInterface? = block(e)
        }
}
