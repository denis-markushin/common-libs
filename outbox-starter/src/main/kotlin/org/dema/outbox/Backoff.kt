package org.dema.outbox

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Pause the relay keeps after failed polls. The first failure waits [base],
 * each consecutive one doubles the pause up to [cap], and a clean poll lifts it,
 * so a broker outage costs one blocked send per pause instead of one per poll.
 * Measured on a monotonic [TimeSource], so a wall-clock step cannot stretch it.
 */
internal class Backoff(
    private val base: Duration,
    private val cap: Duration,
    private val time: TimeSource,
) {
    private var pause = Duration.ZERO
    private var resume: TimeMark? = null

    fun ready(): Boolean = resume?.hasPassedNow() ?: true

    fun fail() {
        pause = minOf(cap, maxOf(base, pause * 2))
        resume = time.markNow() + pause
    }

    fun clear() {
        pause = Duration.ZERO
        resume = null
    }
}
