package org.demalab.servicecore

import assertk.assertThat
import assertk.assertions.isEmpty
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Properties

/**
 * Guards the `spring.factories` this module ships.
 *
 * Spring reads the file only inside a consumer application, so a misspelled class name would pass the
 * build and fail the consumer at startup, while a misspelled key would silently drop the registration.
 */
class SpringFactoriesTest {

    @Test
    fun `names only classes that exist`() {
        val factory = Properties().apply { File("src/main/resources/META-INF/spring.factories").bufferedReader().use(::load) }
        val orphans = factory.stringPropertyNames()
            .flatMap { listOf(it) + factory.getProperty(it).split(',') }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { runCatching { Class.forName(it, false, javaClass.classLoader) }.isFailure }
        assertThat(orphans, "spring.factories names classes that do not exist, consumers lose or fail on them").isEmpty()
    }
}
