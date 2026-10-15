package org.dema.outbox

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.TimeGauge
import io.micrometer.core.instrument.binder.MeterBinder
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val log = KotlinLogging.logger {}

/**
 * Gauges operators alert on when the relay stalls: the backlog it still owes
 * Kafka, how long the head of that backlog has waited, the dead events it gave
 * up on and whether a poll that delivered nothing paused it. A scrape never
 * waits for the database: it reads the last [Snapshot] and asks the
 * `outbox-metrics` thread, the only writer of snapshots, for a fresh one, so
 * the counts lag one scrape behind the table; binding starts the first sample
 * before any scrape. While the database is down the counts keep their last
 * value and the age keeps growing from it, so a stuck head still trips its
 * alert. Once closed, the gauges read nothing rather than a backlog nobody
 * samples any more.
 */
internal class OutboxMetrics(
    private val store: OutboxStore,
    private val publisher: OutboxPublisher,
    private val props: OutboxProperties,
    private val time: TimeSource,
) : MeterBinder,
    AutoCloseable {
    private val sampler = Executors.newSingleThreadExecutor { task ->
        Thread(task, "outbox-metrics").apply { isDaemon = true }
    }
    private val sampling = AtomicBoolean()

    @Volatile
    private var last: Snapshot? = null

    private val current: Snapshot?
        get() = last.takeUnless { sampler.isShutdown }

    override fun bindTo(registry: MeterRegistry) {
        Gauge
            .builder("outbox.events.pending", this) {
                it.refresh()
                it.current?.backlog?.pending?.toDouble() ?: Double.NaN
            }.description("Unpublished events the relay still tries to send")
            .strongReference(true)
            .register(registry)
        TimeGauge
            .builder("outbox.events.oldest.age", this, TimeUnit.SECONDS) {
                it.refresh()
                it.current?.age?.toDouble(DurationUnit.SECONDS) ?: Double.NaN
            }.description("Time the oldest pending event has waited for publication")
            .strongReference(true)
            .register(registry)
        Gauge
            .builder("outbox.events.dead", this) {
                it.refresh()
                it.current?.backlog?.dead?.toDouble() ?: Double.NaN
            }.description("Unpublished events that reached max-attempts and left the relay")
            .strongReference(true)
            .register(registry)
        Gauge
            .builder("outbox.relay.paused", publisher) { if (it.paused) 1.0 else 0.0 }
            .description("1 from a relay poll that delivered nothing until one that delivers, 0 otherwise")
            .strongReference(true)
            .register(registry)
        refresh()
    }

    override fun close() {
        sampler.shutdownNow()
    }

    internal fun sample(): Future<*> = sampler.submit { record() }

    private fun refresh() {
        if (!sampler.isShutdown && sampling.compareAndSet(false, true)) {
            sampler.execute {
                try {
                    record()
                } finally {
                    sampling.set(false)
                }
            }
        }
    }

    private fun record() {
        try {
            last = Snapshot(store.backlog(props.maxAttempts), time.markNow())
        } catch (e: Exception) {
            log.warn(e) { "Outbox metrics failed to sample the backlog, so the gauges show the previous sample" }
        }
    }

    /** Backlog as the database saw it at [mark]; the age of a pending head keeps growing from there. */
    private class Snapshot(
        val backlog: Backlog,
        private val mark: TimeMark,
    ) {
        val age: Duration
            get() = backlog.age?.plus(mark.elapsedNow()) ?: Duration.ZERO
    }
}
