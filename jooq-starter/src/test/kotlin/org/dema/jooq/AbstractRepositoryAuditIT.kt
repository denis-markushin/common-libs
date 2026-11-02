package org.dema.jooq

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.dema.jooq.audit.AuditProperties
import org.dema.jooq.audit.AuditSupport
import org.dema.jooq.audit.TaskBase
import org.dema.jooq.audit.TaskRecord
import org.dema.jooq.audit.TaskTable
import org.dema.jooq.audit.TaskTable.Companion.TASK
import org.dema.jooq.audit.actingAs
import org.dema.jooq.audit.user
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

/**
 * Verifies the audit columns on the repository write paths the RecordListener does not see.
 */
class AbstractRepositoryAuditIT {

    private class TaskRepo : AbstractRepository<TaskTable, TaskRecord>(TASK) {

        init {
            dsl = TaskBase.dsl
            ReflectionTestUtils.setField(this, "auditSupport", AuditSupport(AuditProperties()))
        }

        fun stamp(): String? = actor()
    }

    private fun task(id: UUID): TaskRecord = TaskRepo().newRec().apply {
        set(TASK.ID, id)
        set(TASK.PAYLOAD, "payload-${UUID.randomUUID()}")
    }

    private fun actors(id: UUID): Pair<String?, String?> =
        TaskBase.dsl.fetchOne(TASK, TASK.ID.eq(id))!!.let { it.get(TASK.CREATED_BY) to it.get(TASK.UPDATED_BY) }

    @Test
    fun `insertOnConflictDoNothing writes the login to both columns`() {
        val login = "tekhnolog.${UUID.randomUUID()}"
        val id = UUID.randomUUID()
        actingAs(user(login)) { TaskRepo().insertOnConflictDoNothing(task(id)) }
        assertThat(actors(id), "idempotent create lost its author").isEqualTo(login to login)
    }

    @Test
    fun `insertOnConflictDoNothing leaves the winning row alone on conflict`() {
        val author = "kladovshchik.${UUID.randomUUID()}"
        val id = UUID.randomUUID()
        actingAs(user(author)) { TaskRepo().insertOnConflictDoNothing(task(id)) }
        actingAs(user("sopernik.${UUID.randomUUID()}")) { TaskRepo().insertOnConflictDoNothing(task(id)) }
        assertThat(actors(id), "losing insert rewrote the winning row").isEqualTo(author to author)
    }

    @Test
    fun `upsert writes the login to both columns of a new row`() {
        val login = "kadrovik.${UUID.randomUUID()}"
        val id = UUID.randomUUID()
        actingAs(user(login)) { TaskRepo().upsert(task(id)) }
        assertThat(actors(id), "upsert of a new row lost its author").isEqualTo(login to login)
    }

    @Test
    fun `upsert keeps the created column and refreshes the updated column on conflict`() {
        val author = "direktor.${UUID.randomUUID()}"
        val editor = "zamestitel.${UUID.randomUUID()}"
        val id = UUID.randomUUID()
        actingAs(user(author)) { TaskRepo().upsert(task(id)) }
        actingAs(user(editor)) { TaskRepo().upsert(task(id)) }
        assertThat(actors(id), "upsert rewrote the author or lost the editor").isEqualTo(author to editor)
    }

    @Test
    fun `actor names the user for statements the listener cannot see`() {
        val login = "revizor.${UUID.randomUUID()}"
        val actor = actingAs(user(login)) { TaskRepo().stamp() }
        assertThat(actor, "repository lost the user of the request").isEqualTo(login)
    }

    @Test
    fun `repository signatures name no Spring Security type`() {
        val type = AbstractRepository::class.java
        val signature = type.declaredFields.map { it.type } + type.declaredMethods.flatMap { listOf(it.returnType) + it.parameterTypes }
        val names = signature.map { it.name }.filter { it.startsWith("org.springframework.security") }
        assertThat(names, "repository signature names a Spring Security type").isEmpty()
    }
}
