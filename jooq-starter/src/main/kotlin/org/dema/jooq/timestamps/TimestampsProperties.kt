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
    val enabled: Boolean = true,
    val createdAtColumn: String = "created_at",
    val updatedAtColumn: String = "updated_at",
)
