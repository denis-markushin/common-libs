package org.dema.jooq.autoconfigure

import assertk.all
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import assertk.assertions.prop
import org.dema.jooq.audit.AuditProperties
import org.dema.jooq.timestamps.TimestampsProperties
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class JooqStarterAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JooqStarterAutoConfiguration::class.java))

    @Test
    fun `autoconfig registers DefaultConfigurationCustomizer bean with default properties`() {
        runner.run { context ->
            assertThat(context.containsBean("timestampsConfigurationCustomizer")).isTrue()
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
                assertThat(context.containsBean("timestampsConfigurationCustomizer")).isFalse()
            }
    }

    @Test
    fun `autoconfig disables timestampsSupport when property enabled is false`() {
        runner.withPropertyValues("dema.jooq.timestamps.enabled=false")
            .run { context ->
                assertThat(context.containsBean("timestampsSupport")).isFalse()
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
                assertThat(context.getBeansOfType(DefaultConfigurationCustomizer::class.java)).isEmpty()
            }
    }

    @Test
    fun `registers TimestampsSupport bean when enabled`() {
        runner.run { context ->
            assertThat(context.containsBean("timestampsSupport")).isTrue()
        }
    }

    @Test
    fun `registers audit listener by default`() {
        runner.run { context ->
            assertThat(context.containsBean("auditConfigurationCustomizer"), "audit listener was not registered").isTrue()
        }
    }

    @Test
    fun `audit stays away when property enabled is false`() {
        runner.withPropertyValues("dema.jooq.audit.enabled=false")
            .run { context ->
                assertThat(context.containsBean("auditConfigurationCustomizer"), "audit listener ignored the property").isFalse()
            }
    }

    @Test
    fun `audit stays away without Spring Security on the classpath`() {
        runner.withClassLoader(FilteredClassLoader(SecurityContextHolder::class.java))
            .run { context ->
                assertThat(context.containsBean("auditSupport"), "audit started without Spring Security").isFalse()
            }
    }

    @Test
    fun `autoconfig honors custom audit column names`() {
        runner.withPropertyValues("dema.jooq.audit.created-by-column=author", "dema.jooq.audit.updated-by-column=editor")
            .run { context ->
                val props = context.getBean(AuditProperties::class.java)
                assertThat(props.createdByColumn to props.updatedByColumn, "audit ignored custom column names").isEqualTo("author" to "editor")
            }
    }
}
