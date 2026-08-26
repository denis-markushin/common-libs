package org.dema.test.security

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class TestJwtDecoderConfigTest {

    @Test
    fun `returns the presented token value untouched`() {
        val jwt = TestJwtDecoderConfig().jwtDecoder().decode("not-a-real-token.9f31c0.zz")
        assertThat(
            jwt.tokenValue,
            "decoder must echo the presented token instead of substituting one of its own",
        ).isEqualTo("not-a-real-token.9f31c0.zz")
    }

    @Test
    fun `accepts a token that no signature would validate`() {
        val jwt = TestJwtDecoderConfig().jwtDecoder().decode("~~~ garbage ~~~")
        assertThat(
            jwt.subject,
            "decoder must resolve a subject for any input, otherwise secured endpoints cannot be reached in tests",
        ).isEqualTo("00000000-0000-0000-0000-000000000000")
    }
}
