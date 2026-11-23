package org.dema.jooq

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.dema.jooq.audit.TaskBase
import org.dema.jooq.audit.TaskRecord
import org.dema.jooq.audit.TaskTable.Companion.TASK
import org.jooq.SelectConditionStep
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random

/**
 * Verifies offset pagination with its total on a live PostgreSQL instance.
 */
class JooqUtilsIT {

    private fun word(): String = (1..12).map { ('а'..'я').random() }.joinToString("")

    private fun rows(prefix: String, count: Int): List<String> =
        List(count) { "$prefix${word()}" }.onEach { TaskBase.dsl.insertInto(TASK).set(TASK.ID, UUID.randomUUID()).set(TASK.PAYLOAD, it).execute() }

    private fun query(prefix: String): SelectConditionStep<TaskRecord> = TaskBase.dsl.selectFrom(TASK).where(TASK.PAYLOAD.startsWith(prefix))

    @Test
    fun `page past the end reports the total of the query`() {
        val prefix = "${word()}-"
        val count = Random.nextInt(3, 10)
        rows(prefix, count)
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), OffsetBasedPageRequest(count.toLong() + Random.nextLong(0, 5), Random.nextLong(1, 5)), TASK)
        assertThat(page.totalElements, "page past the end lost the total of the query").isEqualTo(count.toLong())
    }
}
