package org.dema.test.graphql

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.random.Random

class SchemaConventionTest {

    @Test
    fun `convention rejects a deviation missing from the debt`(@TempDir dir: Path) {
        val convention = convention(dir, "type Zorg${Random.nextInt(1000)} { name: String }", "")
        val error = runCatching { convention.`schema holds no deviation beyond the known debt`() }.exceptionOrNull()
        assertThat(error, "convention let a deviation through without debt").isNotNull().isInstanceOf<AssertionError>()
    }

    @Test
    fun `convention rejects debt the schema has shed`(@TempDir dir: Path) {
        val type = "Zorg${Random.nextInt(1000)}"
        val convention = convention(dir, "\"Zorg\" type $type { \"Name\" name: String }", "description $type")
        val error = runCatching { convention.`debt holds no deviation the schema has shed`() }.exceptionOrNull()
        assertThat(error, "convention kept debt the schema no longer owes").isNotNull().isInstanceOf<AssertionError>()
    }

    @Test
    fun `convention accepts deviations its debt lists`(@TempDir dir: Path) {
        val type = "Zorg${Random.nextInt(1000)}"
        val convention = convention(dir, "type $type { name: String }", "description $type\ndescription $type.name")
        val error = runCatching {
            convention.`schema holds no deviation beyond the known debt`()
            convention.`debt holds no deviation the schema has shed`()
        }.exceptionOrNull()
        assertThat(error, "convention rejected a schema that matches its debt").isNull()
    }

    private fun convention(dir: Path, sdl: String, lines: String): SchemaConvention {
        dir.resolve("schema").createDirectories().resolve("zorg.graphqls").writeText(sdl)
        dir.resolve("debt.txt").writeText(lines)
        return object : SchemaConvention {
            override val schema: Path = dir.resolve("schema")
            override val debt: Path = dir.resolve("debt.txt")
        }
    }
}
