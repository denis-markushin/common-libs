package org.dema.outbox

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Marker for domain payloads published through the outbox.
 * [eventType] and [aggregateType] are denormalised into table columns and
 * excluded from the JSON payload.
 */
interface OutboxEvent {
    @get:JsonIgnore
    val eventType: String

    @get:JsonIgnore
    val aggregateType: String
}
