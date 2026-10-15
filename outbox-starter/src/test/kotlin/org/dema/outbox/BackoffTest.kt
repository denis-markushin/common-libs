package org.dema.outbox

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

/** Verifies the relay pause doubles on consecutive failures, stops at the cap and clears on a clean poll. */
class BackoffTest {

    @Test
    fun `failed poll is not ready before the first pause elapses`() {
        val base = Random.nextLong(50, 5_000)
        val time = TestTimeSource()
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, time)
        backoff.fail()
        time += (base - 1).milliseconds
        assertThat(backoff.ready(), "relay resumed before the first pause elapsed").isFalse()
    }

    @Test
    fun `failed poll is ready once the first pause elapses`() {
        val base = Random.nextLong(50, 5_000)
        val time = TestTimeSource()
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, time)
        backoff.fail()
        time += base.milliseconds
        assertThat(backoff.ready(), "relay stayed paused past the first pause").isTrue()
    }

    @Test
    fun `consecutive failure doubles the pause`() {
        val base = Random.nextLong(50, 5_000)
        val time = TestTimeSource()
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, time)
        backoff.fail()
        backoff.fail()
        time += (base * 2 - 1).milliseconds
        assertThat(backoff.ready(), "second failure did not double the pause").isFalse()
    }

    @Test
    fun `consecutive failure pause ends after twice the first`() {
        val base = Random.nextLong(50, 5_000)
        val time = TestTimeSource()
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, time)
        backoff.fail()
        backoff.fail()
        time += (base * 2).milliseconds
        assertThat(backoff.ready(), "second failure grew the pause beyond double").isTrue()
    }

    @Test
    fun `pause stops growing at the cap`() {
        val base = Random.nextLong(50, 5_000)
        val cap = base * Random.nextLong(3, 40)
        val time = TestTimeSource()
        val backoff = Backoff(base.milliseconds, cap.milliseconds, time)
        repeat(Random.nextInt(8, 40)) { backoff.fail() }
        time += cap.milliseconds
        assertThat(backoff.ready(), "pause grew beyond the cap").isTrue()
    }

    @Test
    fun `clean poll lifts the pause at once`() {
        val base = Random.nextLong(50, 5_000)
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, TestTimeSource())
        backoff.fail()
        backoff.clear()
        assertThat(backoff.ready(), "clean poll left the relay paused").isTrue()
    }

    @Test
    fun `failure after a clean poll restarts from the first pause`() {
        val base = Random.nextLong(50, 5_000)
        val time = TestTimeSource()
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, time)
        repeat(3) { backoff.fail() }
        backoff.clear()
        backoff.fail()
        time += base.milliseconds
        assertThat(backoff.ready(), "clean poll did not reset the pause growth").isTrue()
    }

    @Test
    fun `relay stays paused past the pause until a clean poll`() {
        val base = Random.nextLong(50, 5_000)
        val time = TestTimeSource()
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, time)
        backoff.fail()
        time += (base * Random.nextLong(1, 40)).milliseconds
        assertThat(backoff.paused, "relay reported healthy before any poll went through").isTrue()
    }

    @Test
    fun `relay with the pause turned off is never paused`() {
        val base = Random.nextLong(50, 5_000)
        val backoff = Backoff(base.milliseconds, Duration.ZERO, TestTimeSource())
        repeat(Random.nextInt(1, 6)) { backoff.fail() }
        assertThat(backoff.paused, "relay with max-backoff-ms 0 reported as paused").isFalse()
    }

    @Test
    fun `relay with the pause turned off polls again at once`() {
        val base = Random.nextLong(50, 5_000)
        val backoff = Backoff(base.milliseconds, Duration.ZERO, TestTimeSource())
        repeat(Random.nextInt(1, 6)) { backoff.fail() }
        assertThat(backoff.ready(), "relay with max-backoff-ms 0 waited before polling again").isTrue()
    }

    @Test
    fun `clean poll ends the relay pause`() {
        val base = Random.nextLong(50, 5_000)
        val backoff = Backoff(base.milliseconds, (base * 16).milliseconds, TestTimeSource())
        repeat(Random.nextInt(1, 6)) { backoff.fail() }
        backoff.clear()
        assertThat(backoff.paused, "clean poll left the relay reported as paused").isFalse()
    }
}
