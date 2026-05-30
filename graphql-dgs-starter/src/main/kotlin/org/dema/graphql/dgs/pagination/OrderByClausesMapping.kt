package org.dema.graphql.dgs.utils

import org.jooq.SortField
import java.util.concurrent.ConcurrentHashMap

/**
 * DSL marker annotation to restrict the scope of the order-by DSL.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class OrderByDsl

/**
 * A utility object that maintains a thread-safe mapping between enum keys and JOOQ [SortField] arrays.
 *
 * Used to register and retrieve sort field configurations for GraphQL or REST order-by clauses.
 */
object OrderByClausesMapping {
    private val mapping = ConcurrentHashMap<Enum<*>, Array<out SortField<*>>>()

    /**
     * Registers new order-by mappings using the DSL scope.
     *
     * Example:
     * ```
     * OrderByClausesMapping.register {
     *     key(MyEnum.NAME).fields(USER.NAME.asc())
     * }
     * ```
     *
     * @param init a lambda used to define key-field mappings.
     */
    fun register(init: OrderByClauseScope.() -> Unit) {
        OrderByClauseScope().apply(init)
    }

    @OrderByDsl
    class OrderByClauseScope {
        /** Associates the given [key] with a set of fields using [KeyScope]. */
        fun key(key: Enum<*>): KeyScope = KeyScope(key)
    }

    @OrderByDsl
    class KeyScope(
        private val key: Enum<*>,
    ) {
        /** Registers the provided [fields] for the current [key]. */
        fun fields(vararg fields: SortField<*>) {
            val prev = mapping.putIfAbsent(key, fields)
            check(prev == null) {
                "Mapping for key '${key::class.simpleName}.$key' already exists: ${prev!!.joinToString()}"
            }
        }
    }

    /**
     * Retrieves the [SortField]s associated with the given enum key.
     *
     * @param key the enum key whose fields are to be retrieved.
     * @return the array of sort fields registered for the key.
     * @throws IllegalStateException if no mapping was found for the key.
     */
    fun getFields(key: Enum<*>): Array<out SortField<*>> = mapping[key]
        ?: error("No mapping found for key: '${key::class.simpleName}.$key'")

    /**
     * Clears all registered mappings.
     *
     * Useful for testing or re-initialization scenarios.
     */
    fun clear() {
        mapping.clear()
    }
}
