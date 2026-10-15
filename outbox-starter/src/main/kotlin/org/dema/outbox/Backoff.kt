package org.dema.outbox

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Pause the relay keeps after polls that found events but delivered none. The
 * first such poll waits [base], no longer than the regular poll interval; each
 * consecutive one doubles the wait up to [cap], and a poll that delivers
 * anything, or finds nothing to send, lifts it, so a broker outage costs one
 * blocked send per pause instead of one per poll. Measured on a monotonic
 * [TimeSource], so a wall-clock step cannot stretch it. The relay stays
 * [paused] from such a poll until one that delivers or finds nothing to send,
 * even once the wait elapsed, so a metrics scrape from another thread sees a
 * broker outage as one unbroken pause; with a zero [cap] it never pauses.
 */
internal class Backoff(
    private val base: Duration,
    private val cap: Duration,
    private val time: TimeSource,
) {
    @Volatile
    private var pause = Duration.ZERO
    private var resume: TimeMark? = null

    val paused: Boolean get() = pause > Duration.ZERO

    fun ready(): Boolean = resume?.hasPassedNow() ?: true

    fun fail() {
        if (cap <= Duration.ZERO) return
        pause = minOf(cap, maxOf(base, pause * 2))
        resume = time.markNow() + pause
    }

    fun clear() {
        pause = Duration.ZERO
        resume = null
    }
}
