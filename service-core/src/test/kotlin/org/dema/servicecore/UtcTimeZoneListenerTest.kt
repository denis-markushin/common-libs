package org.dema.servicecore

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import java.util.TimeZone

class UtcTimeZoneListenerTest {

    @Test
    fun `switches default time zone to utc when application starts`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        SpringApplication(Blank::class.java)
            .run("--spring.main.web-application-type=none", "--spring.main.banner-mode=off")
            .close()
        assertThat(TimeZone.getDefault().id, "default time zone stayed off utc after start").isEqualTo("UTC")
    }

    @Nested
    @SpringBootTest(classes = [Blank::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
    inner class Context {

        init {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
        }

        @Test
        fun `switches default time zone to utc under spring boot test`() {
            assertThat(TimeZone.getDefault().id, "spring boot test context left default time zone off utc").isEqualTo("UTC")
        }
    }

    @SpringBootConfiguration
    class Blank
}
