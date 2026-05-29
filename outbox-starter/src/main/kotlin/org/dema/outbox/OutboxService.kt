package org.dema.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

class OutboxService internal constructor(
    private val store: OutboxStore,
    private val mapper: ObjectMapper,
) {
    fun publish(aggregateId: UUID, payload: OutboxEvent) {
        val env = EventEnvelope(
            eventId = UUID.randomUUID(),
            eventType = payload.eventType,
            aggregateType = payload.aggregateType,
            aggregateId = aggregateId,
            occurredAt = Instant.now(),
            payload = payload,
        )
        store.insert(
            id = env.eventId,
            aggregateType = env.aggregateType,
            aggregateId = env.aggregateId,
            eventType = env.eventType,
            payloadJson = mapper.writeValueAsString(env),
        )
    }
}
