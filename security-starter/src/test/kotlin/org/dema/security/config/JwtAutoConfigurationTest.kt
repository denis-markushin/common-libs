package org.dema.security.config

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.dema.security.jwt.JwtAuthoritiesConverter
import org.dema.security.jwt.SecurityJwtProperties
import org.dema.security.web.JwtAuthenticationEntryPoint
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder

class JwtAutoConfigurationTest {
    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JwtAutoConfiguration::class.java))

    @Test
    fun `backs off when no JwtDecoder present`() {
        runner.run { context ->
            assertThat(context.getBeansOfType(JwtAuthCustomizer::class.java)).isEmpty()
        }
    }

    @Test
    fun `registers jwt customizer when JwtDecoder present`() {
        runner.withBean(JwtDecoder::class.java, { JwtDecoder { _ -> error("decode not used in this test") } })
            .run { context ->
                assertThat(context.containsBean("jwtAuthCustomizer")).isTrue()
            }
    }

    @Test
    fun `honors custom roles claim property`() {
        runner.withBean(JwtDecoder::class.java, { JwtDecoder { _ -> error("decode not used in this test") } })
            .withPropertyValues("dema.security.jwt.roles-claim=groups")
            .run { context ->
                assertThat(context.getBean(SecurityJwtProperties::class.java).rolesClaim).isEqualTo("groups")
            }
    }

    @Test
    fun `backs off jwt customizer when user defines own`() {
        runner
            .withBean(JwtDecoder::class.java, { JwtDecoder { _ -> error("decode not used in this test") } })
            .withBean(
                JwtAuthCustomizer::class.java,
                { JwtAuthCustomizer(JwtAuthoritiesConverter(SecurityJwtProperties()), JwtAuthenticationEntryPoint()) },
            )
            .run { context ->
                assertThat(context.getBeansOfType(JwtAuthCustomizer::class.java)).hasSize(1)
            }
    }
}
