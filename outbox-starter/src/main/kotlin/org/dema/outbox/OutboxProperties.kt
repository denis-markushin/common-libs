package org.dema.outbox

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("dema.outbox")
data class OutboxProperties(
    /** Master switch; set false to turn the whole starter off. */
    var enabled: Boolean = true,
    /** Kafka topic events are published to. Required. */
    var topic: String = "",
    /** Scheduler poll interval (ms). */
    var pollIntervalMs: Long = 1000,
    /** Rows fetched per poll. */
    var batchSize: Int = 100,
    /** Event becomes dead (excluded from fetch) after this many failed sends. */
    var maxAttempts: Int = 5,
    /** Max time to wait for a single Kafka send acknowledgement (ms). */
    var sendTimeoutMs: Long = 10000,
    /** Bundled changelog that provisions the outbox_events table. */
    var liquibase: Liquibase = Liquibase(),
) {
    data class Liquibase(
        /** Auto-create the outbox_events table on startup. */
        var enabled: Boolean = true,
    )
}
