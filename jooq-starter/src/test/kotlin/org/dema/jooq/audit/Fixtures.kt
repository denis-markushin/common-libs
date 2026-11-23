package org.dema.jooq.audit

import org.jooq.CloseableDSLContext
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl
import org.jooq.impl.UpdatableRecordImpl
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Record of the task table, declared the way jOOQ generates it: records of meta tables cannot be stored.
 */
class TaskRecord : UpdatableRecordImpl<TaskRecord>(TaskTable.TASK)

/**
 * Table with audit columns and a primary key.
 */
class TaskTable : TableImpl<TaskRecord>(DSL.name("task")) {

    val ID: TableField<TaskRecord, UUID?> = createField(DSL.name("id"), SQLDataType.UUID.nullable(false), this, "")
    val PAYLOAD: TableField<TaskRecord, String?> = createField(DSL.name("payload"), SQLDataType.CLOB, this, "")
    val CREATED_BY: TableField<TaskRecord, String?> = createField(DSL.name("created_by"), SQLDataType.CLOB, this, "")
    val UPDATED_BY: TableField<TaskRecord, String?> = createField(DSL.name("updated_by"), SQLDataType.CLOB, this, "")
    val CREATED_AT: TableField<TaskRecord, OffsetDateTime?> = createField(DSL.name("created_at"), SQLDataType.TIMESTAMPWITHTIMEZONE, this, "")
    val UPDATED_AT: TableField<TaskRecord, OffsetDateTime?> = createField(DSL.name("updated_at"), SQLDataType.TIMESTAMPWITHTIMEZONE, this, "")

    override fun getRecordType(): Class<TaskRecord> = TaskRecord::class.java

    override fun getPrimaryKey(): UniqueKey<TaskRecord> = Internal.createUniqueKey(this, DSL.name("task_pkey"), arrayOf(ID), true)

    companion object {
        val TASK = TaskTable()
    }
}

/**
 * PostgreSQL of the audit tests, started on first use and gone with the JVM.
 */
object TaskBase {

    private val postgres by lazy { PostgreSQLContainer("postgres:15-alpine").apply { start() } }

    val dsl: DSLContext by lazy {
        connect().also {
            it.execute("create table task (id uuid primary key, payload text, created_by text, updated_by text, created_at timestamptz, updated_at timestamptz)")
        }
    }

    fun connect(): CloseableDSLContext = DSL.using(postgres.jdbcUrl, postgres.username, postgres.password)
}

fun user(login: String): Authentication = JwtAuthenticationToken(token(login), emptyList())

fun token(login: String?): Jwt {
    val builder = Jwt.withTokenValue("token")
        .header("alg", "RS256")
        .subject(UUID.randomUUID().toString())
        .issuedAt(Instant.now())
    login?.let { builder.claim("preferred_username", it) }
    return builder.build()
}

fun swapped(jwt: Jwt): Authentication = UsernamePasswordAuthenticationToken(jwt, null, emptyList())

fun <T> actingAs(authentication: Authentication?, block: () -> T): T {
    SecurityContextHolder.getContext().authentication = authentication
    return try {
        block()
    } finally {
        SecurityContextHolder.clearContext()
    }
}
