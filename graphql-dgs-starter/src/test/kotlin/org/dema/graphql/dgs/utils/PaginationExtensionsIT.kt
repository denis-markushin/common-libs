package org.dema.graphql.dgs.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import graphql.relay.Connection
import org.dema.graphql.dgs.pagination.RelayPageable
import org.dema.graphql.dgs.utils.NoteTable.Companion.NOTE
import org.dema.jooq.AbstractRepository
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl
import org.jooq.impl.UpdatableRecordImpl
import org.junit.jupiter.api.Test
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.UUID

/**
 * Record of the note table, declared the way jOOQ generates it: records of meta tables cannot be stored.
 */
class NoteRecord : UpdatableRecordImpl<NoteRecord>(NOTE)

/**
 * Table of notes with a soft-delete flag for the base condition and a topic for the filter.
 */
class NoteTable : TableImpl<NoteRecord>(DSL.name("note")) {

    val ID: TableField<NoteRecord, UUID?> = createField(DSL.name("id"), SQLDataType.UUID.nullable(false), this, "")
    val TOPIC: TableField<NoteRecord, String?> = createField(DSL.name("topic"), SQLDataType.CLOB, this, "")
    val DELETED: TableField<NoteRecord, Boolean?> = createField(DSL.name("deleted"), SQLDataType.BOOLEAN.nullable(false), this, "")

    override fun getRecordType(): Class<NoteRecord> = NoteRecord::class.java

    override fun getPrimaryKey(): UniqueKey<NoteRecord> = Internal.createUniqueKey(this, DSL.name("note_pkey"), arrayOf(ID), true)

    companion object {
        val NOTE = NoteTable()
    }
}

/**
 * Sorting of note pages, registered once with the enum itself: the mapping registry refuses a second registration.
 */
enum class NoteSort {
    ID_ASC,
    ;

    companion object {
        init {
            OrderByClausesMapping.register { key(ID_ASC).fields(NOTE.ID.asc()) }
        }
    }
}

/**
 * PostgreSQL of the page tests, started on first use and gone with the JVM.
 */
object NoteBase {

    val dsl: DSLContext by lazy {
        val postgres = PostgreSQLContainer("postgres:15-alpine").apply { start() }
        DSL.using(postgres.jdbcUrl, postgres.username, postgres.password).also {
            it.execute("create table note (id uuid primary key, topic text, deleted boolean not null)")
        }
    }
}

/**
 * Repository of live notes: its base condition hides deleted ones.
 */
class NoteRepo : AbstractRepository<NoteTable, NoteRecord>(NOTE, NOTE.DELETED.isFalse) {

    init {
        dsl = NoteBase.dsl
    }
}

/**
 * GraphQL-side note the page converts records into.
 */
data class Note(
    val id: UUID,
    val topic: String,
)

/**
 * Filter of notes by topic.
 */
data class Topic(
    val name: String,
)

fun conversions(): ConversionService = DefaultConversionService().apply {
    addConverter(Topic::class.java, Condition::class.java) { NOTE.TOPIC.eq(it.name) }
    addConverter(NoteRecord::class.java, Note::class.java) { Note(it.get(NOTE.ID)!!, it.get(NOTE.TOPIC)!!) }
}

fun note(topic: String, deleted: Boolean): UUID = UUID.randomUUID().also {
    NoteBase.dsl.insertInto(NOTE).set(NOTE.ID, it).set(NOTE.TOPIC, topic).set(NOTE.DELETED, deleted).execute()
}

/**
 * Repository page converted by the ConversionService: the base condition without a filter and the filter turned into a condition.
 */
class PaginationExtensionsIT {

    @Test
    fun `page without filter keeps only the base condition`() {
        val topic = "topic-${UUID.randomUUID()}"
        val live = note(topic, false)
        val gone = note(topic, true)
        val page: Connection<Note> = NoteRepo().page(conversions(), RelayPageable(sortingKey = NoteSort.ID_ASC, after = null, first = 100_000))
        assertThat(page.edges.map { it.node.id }.filter { it == live || it == gone }, "page without filter showed a deleted note or lost a live one").isEqualTo(listOf(live))
    }

    @Test
    fun `page narrows records by the converted filter`() {
        val topic = "topic-${UUID.randomUUID()}"
        val own = note(topic, false)
        note("topic-${UUID.randomUUID()}", false)
        val page: Connection<Note> = NoteRepo().page(conversions(), RelayPageable(sortingKey = NoteSort.ID_ASC, after = null, first = 100), Topic(topic))
        assertThat(page.edges.map { it.node.id }, "filter took a stranger note or missed its own").isEqualTo(listOf(own))
    }
}
