package org.dema.jooq

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.dema.jooq.audit.TaskBase
import org.dema.jooq.audit.TaskRecord
import org.dema.jooq.audit.TaskTable
import org.dema.jooq.audit.TaskTable.Companion.TASK
import org.dema.jooq.timestamps.TimestampsProperties
import org.dema.jooq.timestamps.TimestampsSupport
import org.junit.jupiter.api.Test
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.random.Random

/**
 * Verifies the timestamp columns on the repository write paths the RecordListener does not see.
 */
class AbstractRepositoryTimestampsIT {

    private class TaskRepo(
        moment: Instant,
    ) : AbstractRepository<TaskTable, TaskRecord>(TASK) {

        init {
            dsl = TaskBase.dsl
            ReflectionTestUtils.setField(this, "timestampsSupport", TimestampsSupport(Clock.fixed(moment, ZoneOffset.UTC), TimestampsProperties()))
        }
    }

    private fun moment(): Instant = Instant.ofEpochSecond(Random.nextLong(1_600_000_000, 1_900_000_000))

    private fun task(repo: TaskRepo, id: UUID): TaskRecord = repo.newRec().apply {
        set(TASK.ID, id)
        set(TASK.PAYLOAD, "ostatok-${UUID.randomUUID()}")
    }

    private fun created(id: UUID): Instant? = TaskBase.dsl.fetchOne(TASK, TASK.ID.eq(id))!!.get(TASK.CREATED_AT)?.toInstant()

    @Test
    fun `insertOnConflictDoNothing fills the created column`() {
        val moment = moment()
        val id = UUID.randomUUID()
        val repo = TaskRepo(moment)
        repo.insertOnConflictDoNothing(task(repo, id))
        assertThat(created(id), "idempotent create left its creation time empty").isEqualTo(moment)
    }

    @Test
    fun `insertOnConflictDoNothing leaves the created column of the winning row alone`() {
        val first = moment()
        val id = UUID.randomUUID()
        val winner = TaskRepo(first)
        winner.insertOnConflictDoNothing(task(winner, id))
        val loser = TaskRepo(moment())
        loser.insertOnConflictDoNothing(task(loser, id))
        assertThat(created(id), "losing insert rewrote the creation time").isEqualTo(first)
    }
}
