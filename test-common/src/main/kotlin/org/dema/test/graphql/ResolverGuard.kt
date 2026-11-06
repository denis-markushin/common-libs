package org.dema.test.graphql

import assertk.assertThat
import assertk.assertions.isEmpty
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsData
import com.netflix.graphql.dgs.DgsEntityFetcher
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsSubscription
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.security.access.expression.ExpressionUtils
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.util.SimpleMethodInvocation
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.util.function.Supplier

/**
 * Default protection of GraphQL resolvers where /graphql is open to anonymous callers and
 * @PreAuthorize is the only barrier. A resolver class without a guard of its own would open its
 * fields to any caller once its type gains a new path, from another class or through federation.
 * A federation entity fetcher is a new root without a namespace guard, so the method carries its own.
 *
 * Every guard expression is evaluated for an anonymous call: a guard that lets it in (permitAll,
 * true, "... or permitAll()") counts as missing, including on a method overriding a strict class guard.
 */
interface ResolverGuard {
    @Test
    fun `every resolver class carries its own guard`(context: ApplicationContext) {
        val offenders = types(context)
            .filter { type -> type.declaredMethods.any(::resolver) }
            .filter { type -> open(type, type.declaredMethods.first(::resolver)) }
            .map { it.name }
        assertThat(offenders, "resolver class is open without an authority check").isEmpty()
    }

    @Test
    fun `no resolver method guard lets an anonymous caller in`(context: ApplicationContext) {
        val offenders = types(context)
            .flatMap { type -> type.declaredMethods.filter(::resolver) }
            .filter { method -> method.getAnnotation(PreAuthorize::class.java)?.value?.let { admits(it, method) } == true }
            .map { "${it.declaringClass.name}.${it.name}" }
        assertThat(offenders, "resolver method guard lets an anonymous caller in").isEmpty()
    }

    @Test
    fun `every entity fetcher carries its own guard`(context: ApplicationContext) {
        val offenders = types(context)
            .flatMap { it.declaredMethods.toList() }
            .filter { it.isAnnotationPresent(DgsEntityFetcher::class.java) }
            .filter { open(it, it) }
            .map { "${it.declaringClass.name}.${it.name}" }
        assertThat(offenders, "federation entity fetcher is open without an authority check of its own").isEmpty()
    }

    private fun types(context: ApplicationContext): List<Class<*>> =
        context.getBeansWithAnnotation(DgsComponent::class.java).values.map { AopUtils.getTargetClass(it) }

    private fun resolver(method: Method): Boolean =
        listOf(DgsData::class.java, DgsQuery::class.java, DgsMutation::class.java, DgsSubscription::class.java)
            .any { MergedAnnotations.from(method).isPresent(it) }

    private fun open(element: AnnotatedElement, method: Method): Boolean =
        element.getAnnotation(PreAuthorize::class.java)?.value?.let { admits(it, method) } ?: true

    private fun admits(expression: String, method: Method): Boolean {
        val handler = DefaultMethodSecurityExpressionHandler()
        val anonymous: Authentication = AnonymousAuthenticationToken("anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))
        val evaluation = handler.createEvaluationContext(Supplier { anonymous }, SimpleMethodInvocation(null, method))
        return ExpressionUtils.evaluateAsBoolean(handler.expressionParser.parseExpression(expression), evaluation)
    }
}
