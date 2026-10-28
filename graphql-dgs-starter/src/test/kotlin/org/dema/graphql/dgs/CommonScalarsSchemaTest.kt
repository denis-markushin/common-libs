package org.dema.graphql.dgs

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsExtendedScalarsAutoConfiguration
import com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.LocalDate
import kotlin.random.Random

class CommonScalarsSchemaTest {
    @Test
    fun `starter schema serves Date as ISO local date`() {
        val day = LocalDate.of(Random.nextInt(1900, 2100), Random.nextInt(1, 13), Random.nextInt(1, 29))
        ApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    JacksonAutoConfiguration::class.java,
                    GraphQlAutoConfiguration::class.java,
                    DgsSpringGraphQLAutoConfiguration::class.java,
                    DgsExtendedScalarsAutoConfiguration::class.java,
                ),
            )
            .withPropertyValues("dgs.graphql.schema-locations=classpath:schema/common-scalars.graphqls")
            .withBean(TypeDefinitionRegistry::class.java, { SchemaParser().parse("type Query { day: Date }") })
            .withBean(DayFetcher::class.java, { DayFetcher(day) })
            .run { ctx ->
                assertThat(
                    ctx.getBean(DgsQueryExecutor::class.java).executeAndExtractJsonPath<String>("{ day }", "data.day"),
                    "Date from the starter schema did not come back as ISO date",
                ).isEqualTo(day.toString())
            }
    }
}

@DgsComponent
class DayFetcher(
    private val day: LocalDate,
) {
    @DgsQuery
    fun day(): LocalDate = day
}
