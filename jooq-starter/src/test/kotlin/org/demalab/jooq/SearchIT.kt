package org.demalab.jooq

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.demalab.jooq.audit.TaskBase
import org.jooq.impl.DSL
import org.junit.jupiter.api.Test

/**
 * Verifies the list search condition on a live PostgreSQL instance: only the database tells whether
 * a pattern character of the query was escaped.
 */
class SearchIT {

    private fun word(): String = (1..12).map { ('а'..'я').random() }.joinToString("")

    private fun found(query: String?, vararg texts: String): Boolean =
        TaskBase.dsl.fetchExists(DSL.selectOne().where(search(query, *texts.map { DSL.inline(it) }.toTypedArray())))

    @Test
    fun `finds a substring ignoring case`() {
        val word = word()
        assertThat(found(word.uppercase(), "${word()} $word${word()}"), "search missed text that differs only by case").isTrue()
    }

    @Test
    fun `finds text in any of the fields`() {
        val word = word()
        assertThat(found(word, word(), "${word()}$word"), "search looked at the first field only").isTrue()
    }

    @Test
    fun `ignores spaces around the query`() {
        val word = word()
        assertThat(found(" \t$word  ", "${word()}$word${word()}"), "search kept the spaces around the query").isTrue()
    }

    @Test
    fun `null query narrows nothing`() {
        assertThat(search(null, DSL.inline(word())), "search without a query narrowed the list").isEqualTo(DSL.noCondition())
    }

    @Test
    fun `blank query narrows nothing`() {
        assertThat(search(" \t ", DSL.inline(word())), "search by a blank query narrowed the list").isEqualTo(DSL.noCondition())
    }

    @Test
    fun `percent sign is not a wildcard`() {
        val head = word()
        val tail = word()
        assertThat(found("$head%$tail", "$head${word()}$tail"), "percent sign of the query matched any text").isFalse()
    }

    @Test
    fun `underscore is not a wildcard`() {
        val head = word()
        val tail = word()
        assertThat(found("${head}_$tail", "${head}я$tail"), "underscore of the query matched any character").isFalse()
    }

    @Test
    fun `backslash is not an escape`() {
        val head = word()
        val tail = word()
        assertThat(found("$head\\$tail", "${word()}$head\\$tail"), "backslash of the query did not match itself").isTrue()
    }
}
