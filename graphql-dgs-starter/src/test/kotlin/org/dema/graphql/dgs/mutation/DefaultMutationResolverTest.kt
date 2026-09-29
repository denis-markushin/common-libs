package org.dema.graphql.dgs.mutation

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.netflix.graphql.dgs.exceptions.DgsException
import graphql.language.Field
import graphql.schema.DataFetchingEnvironment
import graphql.schema.DataFetchingFieldSelectionSet
import io.mockk.every
import io.mockk.mockk
import org.dema.graphql.dgs.error.RuntimeError
import org.dema.graphql.dgs.error.exception.ConflictException
import org.dema.graphql.dgs.error.exception.DomainValidationException
import org.dema.graphql.dgs.error.exception.EntityNotFoundException
import org.dema.graphql.dgs.error.exception.ForbiddenException
import org.dema.graphql.dgs.error.exception.ServiceUnavailableException
import org.dema.graphql.dgs.error.exception.UnauthorizedException
import org.dema.graphql.dgs.error.mapper.CompositeGraphQLErrorMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.slf4j.LoggerFactory
import java.util.UUID
import org.slf4j.event.Level as EventLevel

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

    @Test
    fun `failure without error selected writes no log event`() {
        val dfe = dfeWithErrorSelected(errorSelected = false)
        val events = captured()
        runCatching {
            with(resolver) { dfe.resolveMutation<Int> { throw IllegalStateException("Row ${UUID.randomUUID()}") } }
        }
        assertThat(events.list).isEmpty()
    }

    @Test
    fun `failure with error selected logs unclassified exception at ERROR`() {
        val dfe = dfeWithErrorSelected(errorSelected = true)
        every { errorMapper.toGraphQLError(any()) } returns RuntimeError(message = "Internal server error")
        val events = captured()
        with(resolver) { dfe.resolveMutation<Int> { throw IllegalStateException("Row ${UUID.randomUUID()}") } }
        assertThat(events.list.single().level).isEqualTo(Level.ERROR)
    }

    @Test
    fun `failure with error selected logs DgsException at its own level`() {
        val dfe = dfeWithErrorSelected(errorSelected = true)
        every { errorMapper.toGraphQLError(any()) } returns RuntimeError(message = "Internal server error")
        val events = captured()
        with(resolver) {
            dfe.resolveMutation<Int> {
                throw object : DgsException(message = "Quota ${UUID.randomUUID()}", logLevel = EventLevel.INFO) {}
            }
        }
        assertThat(events.list.single().level).isEqualTo(Level.INFO)
    }

    @Test
    fun `failure with error selected logs the cause`() {
        val dfe = dfeWithErrorSelected(errorSelected = true)
        val message = "Row ${UUID.randomUUID()}"
        every { errorMapper.toGraphQLError(any()) } returns RuntimeError(message = "Internal server error")
        val events = captured()
        with(resolver) { dfe.resolveMutation<Int> { throw IllegalStateException(message) } }
        assertThat(events.list.single().throwableProxy.message).isEqualTo(message)
    }

    @ParameterizedTest
    @MethodSource("starterExceptions")
    fun `failure with error selected logs starter exception at WARN`(boom: Exception) {
        val dfe = dfeWithErrorSelected(errorSelected = true)
        every { errorMapper.toGraphQLError(any()) } returns RuntimeError(message = "Internal server error")
        val events = captured()
        with(resolver) { dfe.resolveMutation<Int> { throw boom } }
        assertThat(events.list.single().level).isEqualTo(Level.WARN)
    }

    companion object {
        @JvmStatic
        fun starterExceptions(): List<Exception> =
            listOf(
                ConflictException(message = "Seat ${UUID.randomUUID()} taken", reason = "SEAT_TAKEN"),
                DomainValidationException(message = "IBAN ${UUID.randomUUID()} malformed", path = "iban"),
                EntityNotFoundException(entityType = "Invoice", entityId = UUID.randomUUID()),
                ForbiddenException(message = "Payroll ${UUID.randomUUID()} locked"),
                ServiceUnavailableException(message = "Ledger ${UUID.randomUUID()} offline", retryAfterSeconds = 17),
                UnauthorizedException(message = "Token ${UUID.randomUUID()} expired"),
            )
    }
}

private fun captured(): ListAppender<ILoggingEvent> =
    ListAppender<ILoggingEvent>().also {
        it.start()
        (LoggerFactory.getLogger("org.dema.graphql.dgs.mutation.DefaultMutationResolver") as Logger).addAppender(it)
    }
