package org.dema.test.graphql

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.walk

/**
 * The schema of a service held against the GraphQL conventions of [Deviations]. Known deviations sit
 * in the debt file, one per line, and the list only shrinks: a new deviation and a debt line with
 * nothing left to forgive fail the build alike. Once the debt is empty, the file can go.
 */
interface SchemaConvention {
    val schema: Path
        get() = Path.of("src/main/resources/schema")

    val debt: Path
        get() = Path.of("src/test/resources/schema-debt.txt")

    @Test
    fun `schema holds no deviation beyond the known debt`() {
        assertThat(deviations() - excuses(), "schema deviates from the conventions beyond the known debt").isEmpty()
    }

    @Test
    fun `debt holds no deviation the schema has shed`() {
        assertThat(excuses() - deviations(), "schema debt forgives deviations the schema no longer has").isEmpty()
    }

    private fun deviations(): Set<String> =
        Deviations.of(schema.walk().filter { it.extension.startsWith("graphql") }.joinToString("\n") { it.readText() })

    private fun excuses(): Set<String> = debt.readLines().toSet()
}
