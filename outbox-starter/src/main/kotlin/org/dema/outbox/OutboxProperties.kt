package org.dema.outbox

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("dema.outbox")
data class OutboxProperties(
    /** Kafka topic events are published to. Required. */
    var topic: String = "",
    /** Scheduler poll interval (ms). */
    var pollIntervalMs: Long = 1000,
    /** Rows fetched per poll. */
    var batchSize: Int = 100,
    /** Event becomes dead (excluded from fetch) after this many failed sends. */
    var maxAttempts: Int = 5,
    var liquibase: Liquibase = Liquibase(),
) {
    data class Liquibase(
        /** Auto-create the outbox_events table on startup. */
        var enabled: Boolean = true,
    )
}
