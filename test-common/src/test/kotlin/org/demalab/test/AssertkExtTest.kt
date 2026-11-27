package org.demalab.test

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.random.Random

class AssertkExtTest {

    @Test
    fun `isCloseToMillis accepts a zoned date time within tolerance`() {
        val within = Random.nextLong(1, 1_000)
        val expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Random.nextLong(0, 4_000_000_000_000)), ZoneId.of("Pacific/Chatham"))
        assertThat(expected.plusNanos(Random.nextLong(0, within * 1_000_000)), "a zoned date time within tolerance read as too far").isCloseToMillis(expected, within)
    }

    @Test
    fun `isCloseToMillis accepts a local date time within tolerance`() {
        val within = Random.nextLong(1, 1_000)
        val expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(Random.nextLong(0, 4_000_000_000_000)), ZoneOffset.UTC)
        assertThat(expected.minusNanos(Random.nextLong(0, within * 1_000_000)), "a local date time within tolerance read as too far").isCloseToMillis(expected, within)
    }

    @Test
    fun `isCloseToMillis accepts an instant within tolerance`() {
        val within = Random.nextLong(1, 1_000)
        val expected = Instant.ofEpochMilli(Random.nextLong(0, 4_000_000_000_000))
        assertThat(expected.plusMillis(Random.nextLong(0, within)), "an instant within tolerance read as too far").isCloseToMillis(expected, within)
    }

    @Test
    fun `isCloseToMillis accepts an offset date time within tolerance`() {
        val within = Random.nextLong(1, 1_000)
        val expected = OffsetDateTime.ofInstant(Instant.ofEpochMilli(Random.nextLong(0, 4_000_000_000_000)), ZoneOffset.ofHoursMinutes(5, 45))
        assertThat(expected.minusNanos(Random.nextLong(0, within * 1_000_000)), "an offset date time within tolerance read as too far").isCloseToMillis(expected, within)
    }

    @Test
    fun `isCloseToMillis rejects an instant beyond tolerance`() {
        val within = Random.nextLong(1, 1_000)
        val expected = Instant.ofEpochMilli(Random.nextLong(0, 4_000_000_000_000))
        val actual = expected.plusMillis(within + Random.nextLong(1, 1_000))
        assertThat(runCatching { assertThat(actual).isCloseToMillis(expected, within) }.isFailure, "an instant beyond tolerance passed as close").isTrue()
    }

    @Test
    fun `isCloseToMillis binds its type to itself so mixed temporals do not compile`() {
        val method = Class.forName("org.demalab.test.AssertkExtKt").methods.single { it.name == "isCloseToMillis" }
        assertThat(method.typeParameters.single().bounds.map { it.typeName }, "isCloseToMillis lets mixed temporals compile").contains("java.lang.Comparable<? super T>")
    }
}
