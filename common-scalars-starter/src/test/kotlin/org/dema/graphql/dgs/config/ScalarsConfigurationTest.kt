package org.dema.graphql.dgs.config

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.InputArgument
import com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.random.Random

class ScalarsConfigurationTest {
    @Test
    fun `query argument of type Phone arrives normalized`() {
        val number = (1..10).map { Random.nextInt(10) }.joinToString("")
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    GraphQlAutoConfiguration::class.java,
                    DgsSpringGraphQLAutoConfiguration::class.java,
                    ScalarsConfiguration::class.java,
                ),
            )
            .withPropertyValues("dgs.graphql.schema-locations=classpath:schema/common-scalars.graphqls")
            .withBean(TypeDefinitionRegistry::class.java, { SchemaParser().parse("type Query { phone(value: Phone!): Phone! email(value: Email!): Email! }") })
            .withBean(EchoFetcher::class.java)
            .run { ctx ->
                assertThat(
                    ctx.getBean(DgsQueryExecutor::class.java)
                        .executeAndExtractJsonPath<String>("{ phone(value: \"+7 (${number.take(3)}) ${number.drop(3)}\") }", "data.phone"),
                    "Phone scalar is not wired into the schema",
                ).isEqualTo("+7$number")
            }
    }

    @Test
    fun `query argument of type Email arrives trimmed`() {
        val email = "${(1..6).map { ('a'..'z').random() }.joinToString("")}@mail.ru"
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    GraphQlAutoConfiguration::class.java,
                    DgsSpringGraphQLAutoConfiguration::class.java,
                    ScalarsConfiguration::class.java,
                ),
            )
            .withPropertyValues("dgs.graphql.schema-locations=classpath:schema/common-scalars.graphqls")
            .withBean(TypeDefinitionRegistry::class.java, { SchemaParser().parse("type Query { phone(value: Phone!): Phone! email(value: Email!): Email! }") })
            .withBean(EchoFetcher::class.java)
            .run { ctx ->
                assertThat(
                    ctx.getBean(DgsQueryExecutor::class.java).executeAndExtractJsonPath<String>("{ email(value: \" $email \") }", "data.email"),
                    "Email scalar is not wired into the schema",
                ).isEqualTo(email)
            }
    }
}

@DgsComponent
class EchoFetcher {
    @DgsQuery
    fun phone(@InputArgument value: String): String = value

    @DgsQuery
    fun email(@InputArgument value: String): String = value
}
