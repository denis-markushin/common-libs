package org.dema.jooq.audit

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the audit actor columns.
 *
 * Prefix: `dema.jooq.audit`.
 *
 * @author Denis Markushin
 */
@ConfigurationProperties(prefix = "dema.jooq.audit")
data class AuditProperties(
    /**
     * Whether audit actor population is enabled.
     */
    val enabled: Boolean = true,
    /**
     * Name of the column set once on a new row and preserved afterwards.
     */
    val createdByColumn: String = "created_by",
    /**
     * Name of the column overwritten on every insert and update.
     */
    val updatedByColumn: String = "updated_by",
)
