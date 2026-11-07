package org.dema.test.graphql

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsEntityFetcher
import com.netflix.graphql.dgs.DgsQuery
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.security.access.prepost.PreAuthorize

class ResolverGuardTest {

    @Test
    fun `guard rejects a resolver class without a guard`() {
        val error = failure(Bare::class.java) { guard, context -> guard.`every resolver class carries its own guard`(context) }
        assertThat(error, "resolver class without a guard slipped through").isNotNull().isInstanceOf<AssertionError>()
    }

    @Test
    fun `guard rejects a method guard that admits an anonymous caller`() {
        val error = failure(Lenient::class.java) { guard, context -> guard.`no resolver method guard lets an anonymous caller in`(context) }
        assertThat(error, "method guard admitting an anonymous caller slipped through").isNotNull().isInstanceOf<AssertionError>()
    }

    @Test
    fun `guard rejects an entity fetcher without its own guard`() {
        val error = failure(Stray::class.java) { guard, context -> guard.`every entity fetcher carries its own guard`(context) }
        assertThat(error, "entity fetcher without its own guard slipped through").isNotNull().isInstanceOf<AssertionError>()
    }

    @Test
    fun `guard accepts guarded resolvers and fetchers`() {
        val error = failure(Guarded::class.java) { guard, context ->
            guard.`every resolver class carries its own guard`(context)
            guard.`no resolver method guard lets an anonymous caller in`(context)
            guard.`every entity fetcher carries its own guard`(context)
        }
        assertThat(error, "guard rejected resolvers that carry their guards").isNull()
    }

    private fun failure(type: Class<*>, check: (ResolverGuard, ApplicationContext) -> Unit): Throwable? =
        AnnotationConfigApplicationContext(type).use { context ->
            runCatching { check(object : ResolverGuard {}, context) }.exceptionOrNull()
        }
}

/** Resolver that forgot its guard. */
@DgsComponent
class Bare {
    @DgsQuery
    fun zorg(): String = "zorg"
}

/** Resolver whose method guard lets an anonymous caller in. */
@DgsComponent
@PreAuthorize("hasAuthority('ZORG_READ')")
class Lenient {
    @DgsQuery
    @PreAuthorize("hasAuthority('ZORG_READ') or permitAll()")
    fun zorg(): String = "zorg"
}

/** Entity fetcher that leans on the class guard instead of its own. */
@DgsComponent
@PreAuthorize("hasAuthority('ZORG_READ')")
class Stray {
    @DgsEntityFetcher(name = "Zorg")
    fun zorg(): String = "zorg"
}

/** Resolver and entity fetcher that carry every guard they need. */
@DgsComponent
@PreAuthorize("hasAuthority('ZORG_READ')")
class Guarded {
    @DgsQuery
    @PreAuthorize("hasAnyAuthority('ZORG_READ', 'ZORG_WRITE')")
    fun zorg(): String = "zorg"

    @DgsEntityFetcher(name = "Zorg")
    @PreAuthorize("hasAuthority('ZORG_READ')")
    fun fetch(): String = "zorg"
}
