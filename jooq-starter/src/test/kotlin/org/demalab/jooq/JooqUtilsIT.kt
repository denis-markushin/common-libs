package org.demalab.jooq

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.demalab.jooq.audit.TaskBase
import org.demalab.jooq.audit.TaskRecord
import org.demalab.jooq.audit.TaskTable.Companion.TASK
import org.jooq.Field
import org.jooq.SelectConditionStep
import org.jooq.SortField
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.util.UUID
import kotlin.random.Random

/**
 * Verifies offset pagination with its total on a live PostgreSQL instance.
 */
class JooqUtilsIT {

    private fun word(): String = (1..12).map { ('a'..'z').random() }.joinToString("")

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

    @Test
    fun `page holds its slice in the order of the sort fields`() {
        val prefix = "${word()}-"
        val all = rows(prefix, Random.nextInt(5, 12))
        val offset = Random.nextInt(0, all.size - 1)
        val size = Random.nextInt(1, all.size - offset + 1)
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), arrayOf<SortField<*>>(TASK.PAYLOAD.desc()), OffsetBasedPageRequest(offset.toLong(), size.toLong()), TASK)
        assertThat(page.content.map { it.get(TASK.PAYLOAD) }, "page lost its slice or its order").isEqualTo(all.sortedDescending().drop(offset).take(size))
    }

    @Test
    fun `spring sort pages in its direction`() {
        val prefix = "${word()}-"
        val all = rows(prefix, Random.nextInt(5, 12))
        val offset = Random.nextInt(0, all.size - 1)
        val size = Random.nextInt(1, all.size - offset + 1)
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), OffsetBasedPageRequest(offset.toLong(), size.toLong(), Sort.by(Sort.Direction.DESC, "payload")), TASK)
        assertThat(page.content.map { it.get(TASK.PAYLOAD) }, "spring sort lost its direction").isEqualTo(all.sortedDescending().drop(offset).take(size))
    }

    @Test
    fun `field sort pages in ascending order`() {
        val prefix = "${word()}-"
        val all = rows(prefix, Random.nextInt(5, 12))
        val offset = Random.nextInt(0, all.size - 1)
        val size = Random.nextInt(1, all.size - offset + 1)
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), arrayOf<Field<*>>(TASK.PAYLOAD), size.toLong(), offset.toLong(), TASK)
        assertThat(page.content.map { it.get(TASK.PAYLOAD) }, "field sort lost the ascending order").isEqualTo(all.sorted().drop(offset).take(size))
    }

    @Test
    fun `unpaged request returns every row in the order of the sort fields`() {
        val prefix = "${word()}-"
        val all = rows(prefix, Random.nextInt(3, 10))
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), arrayOf<Field<*>>(TASK.PAYLOAD), Pageable.unpaged(), TASK)
        assertThat(page.content.map { it.get(TASK.PAYLOAD) }, "unpaged request lost rows or their order").isEqualTo(all.sorted())
    }

    @Test
    fun `unknown sort property is rejected`() {
        val prefix = "${word()}-"
        rows(prefix, Random.nextInt(1, 4))
        val failure = runCatching { JooqUtils.paginate(TaskBase.dsl, query(prefix), OffsetBasedPageRequest(0, 5, Sort.by(word())), TASK) }.exceptionOrNull()
        assertThat(failure, "unknown sort property slipped through").isNotNull().isInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun `first page reports the total of the query`() {
        val prefix = "${word()}-"
        val count = Random.nextInt(3, 10)
        rows(prefix, count)
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), OffsetBasedPageRequest(0, Random.nextLong(1, count.toLong())), TASK)
        assertThat(page.totalElements, "first page lost the total of the query").isEqualTo(count.toLong())
    }

    @Test
    fun `pageable sort orders a page of records`() {
        val prefix = "${word()}-"
        val all = rows(prefix, Random.nextInt(5, 12))
        val offset = Random.nextInt(0, all.size - 1)
        val size = Random.nextInt(1, all.size - offset + 1)
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), OffsetBasedPageRequest(offset.toLong(), size.toLong(), Sort.by(Sort.Direction.DESC, "payload")))
        assertThat(page.content.map { it.get(TASK.PAYLOAD) }, "pageable sort was ignored").isEqualTo(all.sortedDescending().drop(offset).take(size))
    }

    @Test
    fun `first page of a descending sort is not the last page`() {
        val prefix = "${word()}-"
        val count = Random.nextInt(4, 10)
        rows(prefix, count)
        val limit = Random.nextLong(1, count.toLong())
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), arrayOf<SortField<*>>(TASK.PAYLOAD.desc()), limit, 0)
        assertThat(page.map { it.get("last_page", Boolean::class.java) }, "first page of a descending sort claimed to be the last").isEqualTo(List(limit.toInt()) { false })
    }

    @Test
    fun `last page of a query reports that it is the last`() {
        val prefix = "${word()}-"
        val count = Random.nextInt(4, 10)
        rows(prefix, count)
        val limit = Random.nextLong(1, count.toLong())
        val tail = Random.nextLong(1, limit + 1)
        val page = JooqUtils.paginate(TaskBase.dsl, query(prefix), arrayOf<Field<*>>(TASK.PAYLOAD), limit, count - tail)
        assertThat(page.map { it.get("last_page", Boolean::class.java) }, "last page of the query did not know it was the last").isEqualTo(List(tail.toInt()) { true })
    }
}
