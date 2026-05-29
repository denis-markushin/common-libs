package org.dema.outbox

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("dema.outbox")
data class OutboxProperties(
    /** Kafka topic events are published to. Required. */
    val topic: String = "",
    /** Scheduler poll interval (ms). */
    val pollIntervalMs: Long = 1000,
    /** Rows fetched per poll. */
    val batchSize: Int = 100,
    /** Event becomes dead (excluded from fetch) after this many failed sends. */
    val maxAttempts: Int = 5,
    val liquibase: Liquibase = Liquibase(),
) {
    data class Liquibase(
        /** Auto-create the outbox_events table on startup. */
        val enabled: Boolean = true,
    )
}
