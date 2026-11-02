package org.dema.jooq.audit

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.dema.jooq.audit.TaskTable.Companion.TASK
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import java.util.UUID

/**
 * Verifies how [AuditSupport] resolves the actor from the security context and which record columns it fills.
 */
class AuditSupportTest {

    @Test
    fun `actor reads preferred username from the token`() {
        val login = "kladovshik.${UUID.randomUUID()}"
        val actor = actingAs(user(login)) { AuditSupport(AuditProperties()).actor() }
        assertThat(actor, "actor ignored the login of the token").isEqualTo(login)
    }

    @Test
    fun `actor falls back to the subject of the token`() {
        val jwt = token(null)
        val actor = actingAs(swapped(jwt)) { AuditSupport(AuditProperties()).actor() }
        assertThat(actor, "actor lost the subject of a token without a login").isEqualTo(jwt.subject)
    }

    @Test
    fun `actor reads the token held by a swapped authentication`() {
        val login = "bukhgalter.${UUID.randomUUID()}"
        val actor = actingAs(swapped(token(login))) { AuditSupport(AuditProperties()).actor() }
        assertThat(actor, "actor missed the token behind a swapped authentication").isEqualTo(login)
    }

    @Test
    fun `actor takes the name of an authentication without a token`() {
        val name = "x-roles-${UUID.randomUUID()}"
        val authentication = UsernamePasswordAuthenticationToken(name, null, emptyList())
        val actor = actingAs(authentication) { AuditSupport(AuditProperties()).actor() }
        assertThat(actor, "actor lost the name of an authentication without a token").isEqualTo(name)
    }

    @Test
    fun `actor is empty for an anonymous authentication`() {
        val authentication = AnonymousAuthenticationToken("key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))
        val actor = actingAs(authentication) { AuditSupport(AuditProperties()).actor() }
        assertThat(actor, "actor named an anonymous visitor").isNull()
    }

    @Test
    fun `actor is empty without authentication`() {
        val actor = actingAs(null) { AuditSupport(AuditProperties()).actor() }
        assertThat(actor, "actor invented a user out of nothing").isNull()
    }

    @Test
    fun `apply leaves a column that is not text alone`() {
        val id = UUID.randomUUID()
        val task = TaskRecord().apply { set(TASK.ID, id) }
        actingAs(user("sklad.${UUID.randomUUID()}")) { AuditSupport(AuditProperties(updatedByColumn = "id")).apply(task) }
        assertThat(task.get(TASK.ID), "apply wrote a login into a column that is not text").isEqualTo(id)
    }

    @Test
    fun `createdColumn skips a column that is not text`() {
        val column = AuditSupport(AuditProperties(createdByColumn = "id")).createdColumn(TASK)
        assertThat(column, "createdColumn preserved a column that is not text").isNull()
    }
}
