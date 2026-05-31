package org.dema.jooq.autoconfigure

import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.dema.jooq.timestamps.TimestampsProperties
import org.jooq.DSLContext
import org.jooq.RecordListenerProvider
import org.jooq.impl.DefaultRecordListenerProvider
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class JooqStarterAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JooqStarterAutoConfiguration::class.java))

    @Test
    fun `autoconfig registers RecordListenerProvider bean with default properties`() {
        runner.run { context ->
            assertThat(context.getBean(RecordListenerProvider::class.java))
                .isInstanceOf(DefaultRecordListenerProvider::class.java)
        }
    }

    @Test
    fun `autoconfig provides default Clock bean when none defined by user`() {
        runner.run { context ->
            assertThat(context.containsBean("jooqClock")).isTrue()
        }
    }

    @Test
    fun `autoconfig backs off Clock when user defines own Clock bean`() {
        val userClock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC)
        runner.withBean(Clock::class.java, { userClock })
            .run { context ->
                assertThat(context.getBean(Clock::class.java)).isSameInstanceAs(userClock)
            }
    }

    @Test
    fun `autoconfig disables listener when property enabled is false`() {
        runner.withPropertyValues("dema.jooq.timestamps.enabled=false")
            .run { context ->
                assertThat(context.getBeansOfType(RecordListenerProvider::class.java)).isEmpty()
            }
    }

    @Test
    fun `autoconfig honors custom column names from properties`() {
        runner.withPropertyValues(
            "dema.jooq.timestamps.created-at-column=tstamp_created",
            "dema.jooq.timestamps.updated-at-column=tstamp_modified",
        ).run { context ->
            val props = context.getBean(TimestampsProperties::class.java)
            assertThat(props).all {
                prop(TimestampsProperties::createdAtColumn).isEqualTo("tstamp_created")
                prop(TimestampsProperties::updatedAtColumn).isEqualTo("tstamp_modified")
            }
        }
    }

    @Test
    fun `autoconfig backs off when DSLContext is not on classpath`() {
        runner.withClassLoader(FilteredClassLoader(DSLContext::class.java))
            .run { context ->
                assertThat(context.getBeansOfType(RecordListenerProvider::class.java)).isEmpty()
            }
    }
}
