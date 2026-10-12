package org.dema.servicecore

import org.springframework.boot.context.event.ApplicationStartingEvent
import org.springframework.context.ApplicationListener
import java.util.TimeZone

/**
 * Pins the JVM default time zone to UTC before anything else in the
 * application reads it.
 *
 * Columns are `timestamptz`, while code works with `LocalDateTime` through
 * jOOQ converters; both the converters and the pgjdbc session time zone
 * follow the JVM default, so any other zone silently shifts stored times.
 */
class UtcTimeZoneListener : ApplicationListener<ApplicationStartingEvent> {

    override fun onApplicationEvent(event: ApplicationStartingEvent) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}
