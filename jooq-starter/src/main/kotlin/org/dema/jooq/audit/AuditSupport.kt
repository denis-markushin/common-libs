package org.dema.jooq.audit

import org.jooq.Field
import org.jooq.Fields
import org.jooq.Table
import org.jooq.UpdatableRecord
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Single entry point for populating audit actor columns on a jOOQ record.
 *
 * Backs the store path, through the RecordListener, and the repository paths the listener
 * never sees. A row counts as new when the original values of its primary key are empty, not
 * when the created column is: rows older than the column keep it empty and must not be
 * credited to their first editor. A write without a user clears the updated column, so the
 * last change is never attributed to the previous user.
 */
class AuditSupport(
    private val properties: AuditProperties,
) {

    private val trust = AuthenticationTrustResolverImpl()

    fun actor(): String? {
        val authentication = SecurityContextHolder.getContext().authentication?.takeIf(trust::isAuthenticated) ?: return null
        val jwt = authentication.principal as? Jwt ?: return authentication.name
        return jwt.getClaimAsString("preferred_username") ?: jwt.subject
    }

    fun apply(record: UpdatableRecord<*>) {
        val actor = actor()
        column(record, properties.createdByColumn)
            ?.takeIf { fresh(record) && record.get(it) == null }
            ?.let { record.set(it, actor) }
        column(record, properties.updatedByColumn)?.let { record.set(it, actor) }
    }

    fun createdColumn(table: Table<*>): String? = column(table, properties.createdByColumn)?.name

    private fun fresh(record: UpdatableRecord<*>): Boolean = record.key().fields().all { record.original(it) == null }

    @Suppress("UNCHECKED_CAST")
    private fun column(fields: Fields, name: String): Field<String?>? =
        fields.field(name)?.takeIf { it.type == String::class.java } as Field<String?>?
}
