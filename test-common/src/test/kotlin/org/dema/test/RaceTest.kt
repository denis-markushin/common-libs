package org.dema.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RaceTest {

    @Test
    fun `race hands every thread its own index`() {
        val threads = Random.nextInt(2, 9)
        assertThat(Race.run(threads) { it }.sorted(), "racers lost or shared their indexes").isEqualTo((0 until threads).toList())
    }

    @Test
    fun `race runs every thread at once`() {
        val threads = Random.nextInt(2, 9)
        val latch = CountDownLatch(threads)
        val met = Race.run(threads) {
            latch.countDown()
            latch.await(5, TimeUnit.SECONDS)
        }
        assertThat(met, "racers ran one after another instead of together").isEqualTo(List(threads) { true })
    }

    @Test
    fun `race runs every thread as the caller`() {
        val name = "racer".withRandomSuffix()
        val task = FutureTask {
            SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(name, "secret".withRandomSuffix())
            Race.run(4) { SecurityContextHolder.getContext().authentication?.name }
        }
        Thread(task).start()
        assertThat(task.get(30, TimeUnit.SECONDS), "a racer ran without the identity of the caller").isEqualTo(List(4) { name })
    }
}
