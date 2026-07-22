package org.dema.jooq.timestamps

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the timestamps RecordListener.
 *
 * Prefix: `dema.jooq.timestamps`.
 *
 * @author Denis Markushin
 */
@ConfigurationProperties(prefix = "dema.jooq.timestamps")
data class TimestampsProperties(
    /**
     * Whether audit timestamp population is enabled.
     */
    val enabled: Boolean = true,
    /**
     * Name of the column set once when null and preserved on update.
     */
    val createdAtColumn: String = "created_at",
    /**
     * Name of the column overwritten on every insert and update.
     */
    val updatedAtColumn: String = "updated_at",
)
