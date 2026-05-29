package org.dema.outbox

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Transactional
class OutboxPublisher internal constructor(
    private val store: OutboxStore,
    private val kafka: KafkaTemplate<String, String>,
    private val props: OutboxProperties,
) {

    @Scheduled(fixedDelayString = "\${dema.outbox.poll-interval-ms:1000}")
    fun publish() {
        val batch = store.fetchUnpublished(props.batchSize, props.maxAttempts)
        if (batch.isEmpty()) return
        var published = 0
        batch.forEach { row ->
            try {
                kafka.send(props.topic, row.aggregateId.toString(), row.payload).get()
                store.markPublished(row.id)
                published++
            } catch (e: Exception) {
                log.error(e) { "Failed to publish outbox event ${row.id}" }
                store.markFailed(row.id, e.message ?: e.javaClass.name)
            }
        }
        log.debug { "Published $published/${batch.size} outbox event(s) to ${props.topic}" }
    }
}
