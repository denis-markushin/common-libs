package org.dema.outbox

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.SmartLifecycle
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private val log = KotlinLogging.logger {}

/**
 * Relay that drains outbox_events to Kafka on a dedicated `outbox-relay` thread.
 * Owns its scheduling (no global @EnableScheduling) and its transactions
 * (programmatic [TransactionTemplate], no proxy magic).
 */
class OutboxPublisher internal constructor(
    private val store: OutboxStore,
    private val kafka: KafkaTemplate<String, String>,
    private val props: OutboxProperties,
    private val tx: TransactionTemplate,
) : SmartLifecycle {
    private var relay: ScheduledExecutorService? = null

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
        try {
            publish()
        } catch (e: Throwable) {
            log.error(e) { "Outbox relay poll failed" }
        }
    }

    fun publish() {
        tx.executeWithoutResult {
            val batch = store.fetchUnpublished(props.batchSize, props.maxAttempts)
            if (batch.isEmpty()) return@executeWithoutResult
            var published = 0
            for (row in batch) {
                try {
                    kafka.send(props.topic, row.aggregateId.toString(), row.payload)
                        .get(props.sendTimeoutMs, TimeUnit.MILLISECONDS)
                    store.markPublished(row.id)
                    published++
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    store.markFailed(row.id, "Interrupted while sending")
                    break
                } catch (e: TimeoutException) {
                    log.error(e) { "Failed to publish outbox event ${row.id}" }
                    store.markFailed(row.id, "Kafka send timed out after ${props.sendTimeoutMs} ms")
                } catch (e: Exception) {
                    val cause = (e as? ExecutionException)?.cause ?: e
                    log.error(cause) { "Failed to publish outbox event ${row.id}" }
                    store.markFailed(row.id, cause.message ?: cause.javaClass.name)
                }
            }
            log.debug { "Published $published/${batch.size} outbox event(s) to ${props.topic}" }
        }
    }
}
