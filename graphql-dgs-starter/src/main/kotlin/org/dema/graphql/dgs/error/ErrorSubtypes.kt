package org.dema.graphql.dgs.error

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * Registry of the concrete [ErrorInterface] subtypes an application can deserialize, keyed by
 * the GraphQL `__typename` that identifies each one on the wire.
 *
 * The starter's own errors are seeded eagerly, so resolution works before any Spring context
 * exists. Subtypes defined by a consumer are contributed at startup by
 * [org.dema.graphql.dgs.autoconfigure.ErrorInterfaceJacksonAutoConfiguration].
 */
object ErrorSubtypes {

    val BUILT_IN_ERRORS: Set<Class<*>> = setOf(
        NotFoundError::class.java,
        ValidationError::class.java,
        ConflictError::class.java,
        UnauthorizedError::class.java,
        ForbiddenError::class.java,
        ServiceUnavailableError::class.java,
        RuntimeError::class.java,
    )

    private val registry = ConcurrentHashMap<String, Class<*>>()

    init {
        register(BUILT_IN_ERRORS)
    }

    fun register(types: Collection<Class<*>>) {
        types.forEach { type ->
            val previous = registry.put(type.simpleName, type)
            if (previous != null && previous != type) {
                log.warn { "Error subtype ${type.name} overrides ${previous.name} for typename ${type.simpleName}" }
            }
        }
    }

    fun resolve(typename: String): Class<*>? = registry[typename]

    fun typenames(): Set<String> = registry.keys
}
