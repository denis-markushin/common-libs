package org.dema.outbox

import java.time.Instant
import java.util.UUID

data class EventEnvelope(
    val eventId: UUID,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: UUID,
    val occurredAt: Instant,
    val version: String = "1",
    val payload: OutboxEvent,
)
