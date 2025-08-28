package org.dema.test

import assertk.Assert
import assertk.fail
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

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

/**
 * Asserts that the difference between two [LocalDateTime] values is within
 * the given tolerance in milliseconds.
 */
fun Assert<LocalDateTime>.isCloseToMillis(expected: LocalDateTime, within: Long) = given { actual ->
    val diff = abs(ChronoUnit.MILLIS.between(expected, actual))
    if (diff > within) {
        fail("expected to be within <$within ms> of <$expected>, but was <$actual> (diff=$diff ms)")
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

/** Same as [isCloseToMillis] but for [Instant]. */
fun Assert<Instant>.isCloseToMillis(expected: Instant, within: Long) = given { actual ->
    val diff = abs(ChronoUnit.MILLIS.between(expected, actual))
    if (diff > within) {
        fail("expected to be within <$within ms> of <$expected>, but was <$actual> (diff=$diff ms)")
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

/** Same as [isCloseToMillis] but for [OffsetDateTime]. */
fun Assert<OffsetDateTime>.isCloseToMillis(expected: OffsetDateTime, within: Long) = given { actual ->
    val diff = abs(ChronoUnit.MILLIS.between(expected, actual))
    if (diff > within) {
        fail("expected to be within <$within ms> of <$expected>, but was <$actual> (diff=$diff ms)")
    }
}
