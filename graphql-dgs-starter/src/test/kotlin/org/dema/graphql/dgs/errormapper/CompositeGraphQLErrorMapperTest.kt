package org.dema.graphql.dgs.errormapper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.NotFoundError
import org.dema.graphql.dgs.error.RuntimeError
import org.dema.graphql.dgs.error.mapper.CompositeGraphQLErrorMapper
import org.dema.graphql.dgs.error.mapper.GraphQLErrorMapper
import org.junit.jupiter.api.Test

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
    fun `empty mapper list falls back to RuntimeError using exception message`() {
        val composite = CompositeGraphQLErrorMapper(emptyList())
        val result = composite.toGraphQLError(RuntimeException("boom"))
        assertThat(result).isEqualTo(RuntimeError(message = "boom"))
    }

    @Test
    fun `all-null mappers fall back to RuntimeError using class name when message is null`() {
        val composite = CompositeGraphQLErrorMapper(listOf(nullMapper))
        val result = composite.toGraphQLError(RuntimeException())
        assertThat(result).isEqualTo(RuntimeError(message = "RuntimeException"))
    }

    @Test
    fun `falls back to literal Unknown error when message and simple name are both null`() {
        val composite = CompositeGraphQLErrorMapper(emptyList())
        val anonymous: Throwable = object : Throwable() {}
        val result = composite.toGraphQLError(anonymous)
        assertThat(result).isEqualTo(RuntimeError(message = "Unknown error"))
    }

    private fun mapperOf(block: (Throwable) -> ErrorInterface?): GraphQLErrorMapper =
        object : GraphQLErrorMapper {
            override fun map(e: Throwable): ErrorInterface? = block(e)
        }
}
