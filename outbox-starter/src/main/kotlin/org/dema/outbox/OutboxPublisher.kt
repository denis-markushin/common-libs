package org.dema.outbox

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.InvalidRecordException
import org.apache.kafka.common.errors.ApiException
import org.apache.kafka.common.errors.RecordBatchTooLargeException
import org.apache.kafka.common.errors.RecordTooLargeException
import org.apache.kafka.common.errors.SerializationException
import org.springframework.context.SmartLifecycle
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val log = KotlinLogging.logger {}

/**
 * Relay that drains outbox_events to Kafka on a dedicated `outbox-relay` thread.
 * Owns its scheduling (no global @EnableScheduling) and its transactions
 * (programmatic [TransactionTemplate], no proxy magic).
 * A failed send ends the poll, so later events neither overtake it nor queue up
 * behind a dead broker, and pauses the relay with a growing [Backoff]. Only a
 * failure of the event itself (too large, unserializable) counts toward
 * `max-attempts`; broker, network, authentication and ACL failures never kill events.
 * The pause belongs to the relay thread alone: a [publish] from another thread
 * neither waits for it nor lifts it, so no state is shared between threads.
 * Each poll first takes a database lock held until its batch commits, so only
 * one relay publishes at a time across replicas and no batch overtakes
 * another; a relay that finds the lock taken skips the poll unpaused.
 */
class OutboxPublisher internal constructor(
    private val store: OutboxStore,
    private val kafka: KafkaTemplate<String, String>,
    private val props: OutboxProperties,
    private val tx: TransactionTemplate,
    time: TimeSource,
) : SmartLifecycle {
    private var relay: ScheduledExecutorService? = null
    private val backoff = Backoff(props.pollIntervalMs.milliseconds, props.maxBackoffMs.milliseconds, time)

    override fun start() {
        relay = Executors.newSingleThreadScheduledExecutor { task ->
            Thread(task, "outbox-relay").apply { isDaemon = true }
        }.also {
            it.scheduleWithFixedDelay(::tick, props.pollIntervalMs, props.pollIntervalMs, TimeUnit.MILLISECONDS)
        }
    }

    override fun stop() {
        relay?.shutdownNow()
        try {
            if (relay?.awaitTermination(5, TimeUnit.SECONDS) == false) {
                log.warn { "Outbox relay did not terminate within 5s" }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        relay = null
    }

    override fun isRunning(): Boolean = relay != null

    internal fun tick() {
        if (!backoff.ready()) return
        try {
            when (poll()) {
                true -> backoff.clear()
                false -> backoff.fail()
                null -> log.debug { "Outbox relay skipped a poll of ${props.topic} while another relay holds the lock" }
            }
        } catch (e: Throwable) {
            log.error(e) { "Outbox relay poll failed" }
            backoff.fail()
        }
    }

    internal fun publish() {
        poll()
    }

    private fun poll(): Boolean? = tx.execute { if (store.lock()) drain() else null }

    private fun drain(): Boolean {
        val batch = store.fetchUnpublished(props.batchSize, props.maxAttempts)
        if (batch.isEmpty()) return true
        val count = batch.takeWhile(::send).size
        log.debug { "Published $count/${batch.size} outbox event(s) to ${props.topic}" }
        return count == batch.size
    }

    private fun send(row: OutboxRow): Boolean =
        try {
            kafka.send(props.topic, row.aggregateId.toString(), row.payload)
                .get(props.sendTimeoutMs, TimeUnit.MILLISECONDS)
            store.markPublished(row.id)
            true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        } catch (e: TimeoutException) {
            log.error(e) { "Failed to publish outbox event ${row.id}" }
            store.markDeferred(row.id, "Kafka send timed out after ${props.sendTimeoutMs} ms")
            false
        } catch (e: Exception) {
            val chain = generateSequence<Throwable>(e) { it.cause }
            val cause = chain.firstOrNull { it is ApiException } ?: chain.last()
            val error = cause.message ?: cause.javaClass.name
            log.error(e) { "Failed to publish outbox event ${row.id}" }
            if (chain.any { it.rejection }) store.markFailed(row.id, error) else store.markDeferred(row.id, error)
            false
        }

    private val Throwable.rejection: Boolean
        get() = this is RecordTooLargeException || this is RecordBatchTooLargeException ||
            this is SerializationException || this is InvalidRecordException
}
