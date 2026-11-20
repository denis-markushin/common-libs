package org.dema.jooq

import org.jooq.Condition
import org.jooq.Field
import org.jooq.impl.DSL

/**
 * Condition of a list search: a case-insensitive substring in at least one of the fields.
 *
 * The query is trimmed, and a blank one narrows nothing. The pattern characters of the query
 * are escaped, so a percent sign, an underscore and a backslash match only themselves.
 */
fun search(query: String?, vararg fields: Field<String>): Condition =
    query?.trim()?.ifEmpty { null }?.let { term -> DSL.or(fields.map { it.containsIgnoreCase(term) }) } ?: DSL.noCondition()
