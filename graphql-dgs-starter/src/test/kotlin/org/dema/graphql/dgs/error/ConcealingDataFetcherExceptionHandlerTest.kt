package org.dema.graphql.dgs.error

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.netflix.graphql.dgs.exceptions.DefaultDataFetcherExceptionHandler
import com.netflix.graphql.dgs.exceptions.DgsException
import com.netflix.graphql.types.errors.ErrorType
import graphql.GraphQLError
import graphql.Scalars
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.execution.ExecutionStepInfo
import graphql.execution.MergedField
import graphql.execution.ResultPath
import graphql.language.Field
import graphql.schema.DataFetchingEnvironmentImpl
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import java.util.UUID
import java.util.concurrent.TimeUnit

class ConcealingDataFetcherExceptionHandlerTest {

    @Test
    fun `unclassified exception reaches client as internal server error`() {
        val error = handled(IllegalStateException("Failing row contains (${UUID.randomUUID()})"), "orders")
        assertThat(error.message).isEqualTo("Internal server error")
    }

    @Test
    fun `unclassified exception keeps INTERNAL error type`() {
        val error = handled(IllegalArgumentException("Key (email)=(${UUID.randomUUID()}) exists"), "users")
        assertThat(error.errorType).isEqualTo(ErrorType.INTERNAL)
    }

    @Test
    fun `unclassified exception keeps its path`() {
        val error = handled(RuntimeException("${UUID.randomUUID()}"), "invoices")
        assertThat(error.path).isEqualTo(listOf<Any>("invoices"))
    }

    @Test
    fun `DgsException with INTERNAL type keeps its message`() {
        val message = "Ledger ${UUID.randomUUID()} is locked"
        val error = handled(object : DgsException(message = message, errorType = ErrorType.INTERNAL) {}, "ledger")
        assertThat(error.message).isEqualTo(message)
    }

    @Test
    fun `AccessDeniedException keeps PERMISSION_DENIED error type`() {
        val error = handled(AccessDeniedException("${UUID.randomUUID()}"), "payroll")
        assertThat(error.errorType).isEqualTo(ErrorType.PERMISSION_DENIED)
    }
}

private fun handled(exception: Throwable, field: String): GraphQLError =
    ConcealingDataFetcherExceptionHandler(DefaultDataFetcherExceptionHandler())
        .handleException(
            DataFetcherExceptionHandlerParameters.newExceptionParameters()
                .exception(exception)
                .dataFetchingEnvironment(
                    DataFetchingEnvironmentImpl.newDataFetchingEnvironment()
                        .executionStepInfo(
                            ExecutionStepInfo.newExecutionStepInfo()
                                .type(Scalars.GraphQLString)
                                .path(ResultPath.rootPath().segment(field))
                                .build(),
                        )
                        .mergedField(MergedField.newMergedField(Field(field)).build())
                        .build(),
                )
                .build(),
        )
        .get(5, TimeUnit.SECONDS)
        .errors
        .single()
