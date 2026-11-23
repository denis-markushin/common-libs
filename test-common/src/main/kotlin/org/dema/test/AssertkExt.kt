package org.dema.test

import assertk.Assert
import assertk.fail
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.math.abs

/**
 * Asserts that the difference between two [Temporal] values is within
 * the given tolerance in milliseconds.
 */
fun <T : Temporal> Assert<T>.isCloseToMillis(expected: T, within: Long) = given { actual ->
    val diff = abs(ChronoUnit.MILLIS.between(expected, actual))
    if (diff > within) {
        fail("expected to be within <$within ms> of <$expected>, but was <$actual> (diff=$diff ms)")
    }
}

// ========== LocalDateTime ==========

/**
 * Asserts that two [LocalDateTime] values are equal when truncated to milliseconds,
 * effectively ignoring micro/nano differences.
 */
fun Assert<LocalDateTime>.isEqualToMillis(expected: LocalDateTime) = given { actual ->
    val a = actual.truncatedTo(ChronoUnit.MILLIS)
    val e = expected.truncatedTo(ChronoUnit.MILLIS)
    if (!a.equals(e)) {
        fail("expected <$e> (MILLIS precision) but was <$a>")
    }
}

// ========== Instant ==========

/** Same as [isEqualToMillis] but for [Instant]. */
fun Assert<Instant>.isEqualToMillis(expected: Instant) = given { actual ->
    val a = actual.truncatedTo(ChronoUnit.MILLIS)
    val e = expected.truncatedTo(ChronoUnit.MILLIS)
    if (!a.equals(e)) {
        fail("expected <$e> (MILLIS precision) but was <$a>")
    }
}

// ========== OffsetDateTime ==========

/** Same as [isEqualToMillis] but for [OffsetDateTime]. */
fun Assert<OffsetDateTime>.isEqualToMillis(expected: OffsetDateTime) = given { actual ->
    val a = actual.truncatedTo(ChronoUnit.MILLIS)
    val e = expected.truncatedTo(ChronoUnit.MILLIS)
    if (!a.equals(e)) {
        fail("expected <$e> (MILLIS precision) but was <$a>")
    }
}
