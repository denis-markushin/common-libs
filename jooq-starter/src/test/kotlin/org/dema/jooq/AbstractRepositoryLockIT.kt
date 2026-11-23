package org.dema.jooq

import assertk.assertThat
import assertk.assertions.isNotNull
import org.dema.jooq.audit.TaskBase
import org.dema.jooq.audit.TaskRecord
import org.dema.jooq.audit.TaskTable
import org.dema.jooq.audit.TaskTable.Companion.TASK
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Verifies that a locking read waits for the lock another transaction holds instead of
 * passing over the row.
 */
class AbstractRepositoryLockIT {

    private class LockRepo(
        context: DSLContext,
    ) : AbstractRepository<TaskTable, TaskRecord>(TASK) {

        init {
            dsl = context
        }
    }

    private fun waiting(pid: Int): Boolean =
        TaskBase.dsl.fetchExists(
            DSL.selectOne().from(DSL.table("pg_stat_activity")).where(DSL.field("pid").eq(pid), DSL.field("wait_event_type").eq("Lock")),
        )

    private fun blocked(pid: Int) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!waiting(pid) && System.nanoTime() < deadline) {
            Thread.sleep(50)
        }
    }

    @Test
    fun `getOneBy for update waits for a lock held by another transaction`() {
        val id = UUID.randomUUID()
        TaskBase.dsl.insertInto(TASK).set(TASK.ID, id).set(TASK.PAYLOAD, "nakladnaya-${UUID.randomUUID()}").execute()
        val read = TaskBase.connect().use { holder ->
            TaskBase.connect().use { waiter ->
                Executors.newSingleThreadExecutor().use { pool ->
                    val pid = waiter.fetchValue("select pg_backend_pid()") as Int
                    holder.transactionResult { tx ->
                        LockRepo(tx.dsl()).getOneBy(forUpdate = true) { it.ID.eq(id) }
                        pool.submit<TaskRecord?> {
                            waiter.transactionResult { other -> LockRepo(other.dsl()).getOneBy(forUpdate = true) { it.ID.eq(id) } }
                        }.also { blocked(pid) }
                    }.get(10, TimeUnit.SECONDS)
                }
            }
        }
        assertThat(read, "row locked by another transaction read as absent").isNotNull()
    }
}
