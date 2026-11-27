package org.demalab.jooq

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import kotlin.random.Random

class OffsetBasedPageRequestTest {
    @Test
    fun `calculates page number`() {
        val pr = OffsetBasedPageRequest(20, 10)
        assertEquals(2, pr.pageNumber)
        assertEquals(20, pr.offset)
        assertEquals(10, pr.pageSize)
    }

    @Test
    fun `next returns request with increased offset`() {
        val pr = OffsetBasedPageRequest(0, 5, Sort.by("id"))
        val next = pr.next() as OffsetBasedPageRequest
        assertEquals(5, next.offset)
        assertEquals(pr.sort, next.sort)
    }

    @Test
    fun `hasPrevious works`() {
        val pr = OffsetBasedPageRequest(10, 10)
        assertTrue(pr.hasPrevious())
        val first = OffsetBasedPageRequest(0, 10)
        assertFalse(first.hasPrevious())
    }

    @Test
    fun `withPage moves the offset to the start of the page`() {
        val limit = Random.nextLong(1, 50)
        val page = Random.nextInt(0, 100)
        assertThat(OffsetBasedPageRequest(Random.nextLong(0, 1000), limit).withPage(page).offset, "withPage missed the start of the page").isEqualTo(page * limit)
    }

    @Test
    fun `previousOrFirst from inside the first page goes to the first page`() {
        val limit = Random.nextLong(2, 50)
        assertThat(OffsetBasedPageRequest(Random.nextLong(1, limit), limit).previousOrFirst().offset, "previousOrFirst stepped before the first row").isEqualTo(0L)
    }
}
