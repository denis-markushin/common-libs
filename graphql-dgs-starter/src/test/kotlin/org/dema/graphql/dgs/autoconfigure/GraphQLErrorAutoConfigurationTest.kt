package org.dema.graphql.dgs.autoconfigure

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.doesNotContain
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import org.dema.graphql.dgs.error.RuntimeError
import org.dema.graphql.dgs.error.mapper.CompositeGraphQLErrorMapper
import org.dema.graphql.dgs.error.mapper.GraphQLErrorMapper
import org.dema.graphql.dgs.error.mapper.NotFoundErrorMapper
import org.dema.graphql.dgs.mutation.MutationResolver
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.util.UUID

class GraphQLErrorAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                GraphQLErrorAutoConfiguration::class.java,
                SpringSecurityErrorAutoConfiguration::class.java,
            ),
        )

    @Test
    fun `default chain registers seven mappers and core beans`() {
        runner.run { ctx ->
            assertThat(ctx.getBeansOfType(GraphQLErrorMapper::class.java).keys).containsExactlyInAnyOrder(
                "notFoundErrorMapper",
                "conflictErrorMapper",
                "validationErrorMapper",
                "unauthorizedErrorMapper",
                "forbiddenErrorMapper",
                "serviceUnavailableErrorMapper",
                "springSecurityAuthErrorMapper",
            )
            assertThat(ctx.getBean(CompositeGraphQLErrorMapper::class.java))
                .isInstanceOf(CompositeGraphQLErrorMapper::class)
            assertThat(ctx.getBean(MutationResolver::class.java))
                .isInstanceOf(MutationResolver::class)
        }
    }

    @Test
    fun `composite resolves unknown exception to RuntimeError via full default chain`() {
        runner.run { ctx ->
            val composite = ctx.getBean(CompositeGraphQLErrorMapper::class.java)
            val result = composite.toGraphQLError(RuntimeException("x"))
            assertThat(result).isInstanceOf(RuntimeError::class)
        }
    }

    @Test
    fun `SpringSecurityAuthErrorMapper is absent when Spring Security is not on classpath`() {
        runner
            .withClassLoader(FilteredClassLoader("org.springframework.security.core"))
            .run { ctx ->
                assertThat(ctx.getBeansOfType(GraphQLErrorMapper::class.java).keys).containsExactlyInAnyOrder(
                    "notFoundErrorMapper",
                    "conflictErrorMapper",
                    "validationErrorMapper",
                    "unauthorizedErrorMapper",
                    "forbiddenErrorMapper",
                    "serviceUnavailableErrorMapper",
                )
            }
    }

    @Test
    fun `default mapper can be replaced by a user bean with the same name`() {
        val replacement = NotFoundErrorMapper()
        runner.withBean("notFoundErrorMapper", NotFoundErrorMapper::class.java, { replacement }).run { ctx ->
            assertThat(ctx.getBean("notFoundErrorMapper")).isSameInstanceAs(replacement)
        }
    }

    @Test
    fun `query hides unclassified exception message from top-level errors`() {
        val email = "${UUID.randomUUID()}@example.org"
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    GraphQlAutoConfiguration::class.java,
                    DgsSpringGraphQLAutoConfiguration::class.java,
                    GraphQLErrorAutoConfiguration::class.java,
                ),
            )
            .withPropertyValues("dgs.graphql.schema-locations=classpath*:none/*.graphqls")
            .withBean(TypeDefinitionRegistry::class.java, { SchemaParser().parse("type Query { account: String }") })
            .withBean(AccountFetcher::class.java, { AccountFetcher("Detail: Key (email)=($email) already exists") })
            .run { ctx ->
                assertThat(ctx.getBean(DgsQueryExecutor::class.java).execute("{ account }").errors.single().message)
                    .doesNotContain(email)
            }
    }
}

@DgsComponent
class AccountFetcher(
    private val detail: String,
) {
    @DgsQuery
    fun account(): String = throw IllegalStateException(detail)
}
