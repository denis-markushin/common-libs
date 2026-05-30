package org.dema.graphql.dgs.mutation

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet
import io.mockk.every
import io.mockk.mockk
import org.dema.graphql.dgs.error.RuntimeError
import org.dema.graphql.dgs.error.mapper.CompositeGraphQLErrorMapper
import org.junit.jupiter.api.Test

class DefaultMutationResolverTest {

    private val errorMapper = mockk<CompositeGraphQLErrorMapper>()
    private val resolver: MutationResolver = DefaultMutationResolver(errorMapper)

    private fun dfeWithErrorSelected(errorSelected: Boolean): DataFetchingEnvironment {
        val selectionSet = mockk<DataFetchingFieldSelectionSet>()
        every { selectionSet.contains("error") } returns errorSelected
        val field = mockk<Field>()
        every { field.name } returns "create"
        val dfe = mockk<DataFetchingEnvironment>()
        every { dfe.selectionSet } returns selectionSet
        every { dfe.field } returns field
        return dfe
    }

    @Test
    fun `success path returns Success wrapping the block value`() {
        val dfe = dfeWithErrorSelected(errorSelected = false)
        with(resolver) {
            val result = dfe.resolveMutation { 42 }
            assertThat(result).isEqualTo(MutationOutcome.Success(42))
        }
    }

    @Test
    fun `failure with error selected returns Failure using the mapper`() {
        val dfe = dfeWithErrorSelected(errorSelected = true)
        val boom = RuntimeException("boom")
        every { errorMapper.toGraphQLError(boom) } returns RuntimeError(message = "boom")
        with(resolver) {
            val result = dfe.resolveMutation<Int> { throw boom }
            assertThat(result).isInstanceOf(MutationOutcome.Failure::class)
            assertThat((result as MutationOutcome.Failure).error).isEqualTo(RuntimeError(message = "boom"))
        }
    }

    @Test
    fun `failure without error selected rethrows the original exception`() {
        val dfe = dfeWithErrorSelected(errorSelected = false)
        val boom = RuntimeException("boom")
        assertFailure {
            with(resolver) { dfe.resolveMutation<Int> { throw boom } }
        }.isInstanceOf(RuntimeException::class).hasMessage("boom")
    }

    @Test
    fun `CancellationException propagates without being mapped`() {
        val dfe = dfeWithErrorSelected(errorSelected = true)
        val cancel = java.util.concurrent.CancellationException("cancelled")
        assertFailure {
            with(resolver) { dfe.resolveMutation<Int> { throw cancel } }
        }.isInstanceOf(java.util.concurrent.CancellationException::class).hasMessage("cancelled")
    }
}
