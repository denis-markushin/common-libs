package org.dema.graphql.dgs.utils

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import org.dema.graphql.dgs.utils.CursorUtils.decode
import org.dema.graphql.dgs.utils.CursorUtils.encodeToBase64
import org.junit.jupiter.api.Test

class CursorUtilsTest {
    @Test
    fun `encode and decode round trip`() {
        val given = arrayOf("1", "2", "3")
        val encoded = given.encodeToBase64()
        val decoded = encoded.decode()
        assertThat(given).containsExactly(*decoded)
    }

    @Test
    fun `decode null returns empty array`() {
        val decoded = (null as String?).decode()
        assertThat(decoded).isEmpty()
    }
}
