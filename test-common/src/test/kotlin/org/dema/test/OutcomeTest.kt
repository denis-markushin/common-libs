package org.dema.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.dema.graphql.dgs.error.exception.ConflictException
import org.junit.jupiter.api.Test
import kotlin.random.Random

class OutcomeTest {

    @Test
    fun `outcome of a returning call is success`() {
        assertThat(Outcome.of { Random.nextInt() }, "a returning call read as a failure").isEqualTo("SUCCESS")
    }

    @Test
    fun `outcome of a conflict is its reason`() {
        val reason = "DUPLICATE".withRandomSuffix(8)
        assertThat(Outcome.of { throw ConflictException("Zorg is taken", reason) }, "a conflict lost its reason").isEqualTo(reason)
    }

    @Test
    fun `outcome of an unexpected exception is its class name`() {
        assertThat(Outcome.of { error("Zorg broke") }, "an unexpected exception hid behind another name").isEqualTo("IllegalStateException")
    }
}
