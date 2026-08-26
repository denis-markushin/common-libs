package org.dema.test.security

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class WithMockJwtSecurityContextFactoryTest {

    @Test
    fun `exposes annotated roles as authorities`() {
        val context = WithMockJwtSecurityContextFactory()
            .createSecurityContext(WithMockJwt(roles = arrayOf("QUARRY_FOREMAN", "night-shift")))
        assertThat(
            context.authentication!!.authorities.map { it.authority },
            "authorities must mirror the annotated roles verbatim",
        ).containsExactlyInAnyOrder("QUARRY_FOREMAN", "night-shift")
    }

    @Test
    fun `names the principal after the annotated user`() {
        val context = WithMockJwtSecurityContextFactory()
            .createSecurityContext(WithMockJwt(userId = "7b3f1c92-0000-4aaa-9d31-51e0a6c4f8b2"))
        assertThat(
            context.authentication!!.name,
            "principal name must be the annotated user id, not the token value",
        ).isEqualTo("7b3f1c92-0000-4aaa-9d31-51e0a6c4f8b2")
    }

    @Test
    fun `grants no authority when no role is annotated`() {
        val context = WithMockJwtSecurityContextFactory()
            .createSecurityContext(WithMockJwt(userId = "0f9d2e11-1111-4bbb-8c02-3a7d5b9e6014"))
        assertThat(
            context.authentication!!.authorities,
            "an unannotated test must not be granted an authority it never asked for",
        ).isEqualTo(emptyList())
    }
}
