package org.demalab.graphql.dgs.config

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guards the auto-configuration imports this module ships.
 *
 * Spring Boot reads the file only inside a consumer application, while the other tests here take
 * configurations through `AutoConfigurations.of`, so a misspelled class name would pass the build
 * and fail the consumer at startup.
 */
class AutoConfigurationImportsTest {

    @Test
    fun `names only classes that exist`() {
        val orphans = File("src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
            .readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .filter { runCatching { Class.forName(it, false, javaClass.classLoader) }.isFailure }
        assertThat(orphans, "auto-configuration imports name classes that do not exist, consumers fail at startup").isEmpty()
    }
}
