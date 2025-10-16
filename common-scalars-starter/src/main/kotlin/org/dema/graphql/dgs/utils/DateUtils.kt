package org.dema.graphql.dgs.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

object DateUtils {

    const val ZULU_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

    val ZULU_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendPattern(ZULU_DATE_TIME_PATTERN)
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .optionalEnd()
        .appendLiteral('Z')
        .toFormatter()

    fun LocalDateTime.toZuluString(): String = format(ZULU_FORMATTER)

    fun LocalDateTime.toOffsetDateTime(): OffsetDateTime = atOffset(ZoneOffset.UTC)

    fun Instant.toLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneOffset.UTC)

    /**
     * Converts ZULU formatted string into a LocalDateTame using ZULU_DATE_TIME_PATTERN.
     *
     *  @return date time
     **/
    fun String.fromZuluToLocalDateTime(): LocalDateTime = toLocalDateTime(ZULU_FORMATTER)

    /**
     * Converts a string with provided formatter.
     *
     * @param formatter Formatter that will be used for conversion.
     **/
    fun String.toLocalDateTime(formatter: DateTimeFormatter): LocalDateTime = LocalDateTime.parse(this, formatter)
}
