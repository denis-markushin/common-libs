package org.dema.jooq.audit

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.dema.jooq.audit.TaskTable.Companion.TASK
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Verifies the audit columns of rows written through UpdatableRecord.store() on a live PostgreSQL instance.
 */
class AuditRecordListenerIT {

    private fun base(properties: AuditProperties = AuditProperties()): DSLContext =
        TaskBase.dsl.configuration().deriveAppending(AuditRecordListener(AuditSupport(properties))).dsl()

    private fun task(dsl: DSLContext = base()): TaskRecord = dsl.newRecord(TASK).apply {
        set(TASK.ID, UUID.randomUUID())
        set(TASK.PAYLOAD, "payload-${UUID.randomUUID()}")
    }

    private fun actors(id: UUID?): Pair<String?, String?> =
        TaskBase.dsl.fetchOne(TASK, TASK.ID.eq(id))!!.let { it.get(TASK.CREATED_BY) to it.get(TASK.UPDATED_BY) }

    private fun TaskRecord.edit() {
        set(TASK.PAYLOAD, "edited-${UUID.randomUUID()}")
        store()
    }

    @Test
    fun `store of a new row writes the login to both columns`() {
        val login = "ofitsiant.${UUID.randomUUID()}"
        val task = task()
        actingAs(user(login)) { task.store() }
        assertThat(actors(task.get(TASK.ID)), "store of a new row lost its author").isEqualTo(login to login)
    }

    @Test
    fun `store of an existing row changes only the updated column`() {
        val author = "povar.${UUID.randomUUID()}"
        val editor = "barmen.${UUID.randomUUID()}"
        val task = task()
        actingAs(user(author)) { task.store() }
        actingAs(user(editor)) { task.edit() }
        assertThat(actors(task.get(TASK.ID)), "edit rewrote the author or lost the editor").isEqualTo(author to editor)
    }

    @Test
    fun `store of a row older than the columns keeps the created column empty`() {
        val editor = "administrator.${UUID.randomUUID()}"
        val id = UUID.randomUUID()
        TaskBase.dsl.execute("insert into task (id) values ({0})", id)
        val task = base().fetchOne(TASK, TASK.ID.eq(id))!!
        actingAs(user(editor)) { task.edit() }
        assertThat(actors(id), "first editor of an old row was credited as its author").isEqualTo(null to editor)
    }

    @Test
    fun `store without a user clears the updated column`() {
        val author = "menedzher.${UUID.randomUUID()}"
        val task = task()
        actingAs(user(author)) { task.store() }
        actingAs(null) { task.edit() }
        assertThat(actors(task.get(TASK.ID)), "system write stayed credited to the previous user").isEqualTo(author to null)
    }

    @Test
    fun `store keeps an author set by the caller`() {
        val author = "import-${UUID.randomUUID()}"
        val task = task().apply { set(TASK.CREATED_BY, author) }
        actingAs(user("kassir.${UUID.randomUUID()}")) { task.store() }
        assertThat(actors(task.get(TASK.ID)).first, "store overwrote an author set by the caller").isEqualTo(author)
    }

    @Test
    fun `store leaves a table without the columns alone`() {
        val quiet = base(AuditProperties(createdByColumn = "opened_by", updatedByColumn = "closed_by"))
        val task = task(quiet)
        actingAs(user("sous-chef.${UUID.randomUUID()}")) { task.store() }
        assertThat(actors(task.get(TASK.ID)), "listener touched a table without the configured columns").isEqualTo(null to null)
    }
}
