package org.dema.graphql.dgs.autoconfigure

import io.github.oshai.kotlinlogging.KotlinLogging
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ErrorSubtypes
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.util.ClassUtils

private val log = KotlinLogging.logger {}

/**
 * Contributes consumer-defined [ErrorInterface] subtypes to [ErrorSubtypes], so that a typed
 * error payload deserializes back into the concrete subtype wherever it is read.
 *
 * The starter's own errors need no discovery; only the classes a consumer adds are searched for,
 * by scanning every package Spring Boot registered for auto-configuration.
 */
@AutoConfiguration
class ErrorInterfaceJacksonAutoConfiguration(
    private val context: ApplicationContext,
) {
    init {
        ErrorSubtypes.register(discoverCustomErrors())
    }

    private fun discoverCustomErrors(): List<Class<*>> {
        if (!AutoConfigurationPackages.has(context)) return emptyList()
        val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
            addIncludeFilter(AssignableTypeFilter(ErrorInterface::class.java))
            resourceLoader = context
        }
        val classLoader = context.classLoader ?: ClassUtils.getDefaultClassLoader()
        return AutoConfigurationPackages.get(context)
            .flatMap { scanner.findCandidateComponents(it) }
            .mapNotNull { candidate ->
                candidate.beanClassName?.let { className ->
                    runCatching { ClassUtils.forName(className, classLoader) }
                        .onFailure { e -> log.warn(e) { "Failed to load error class $className" } }
                        .getOrNull()
                }
            }
            .filterNot { it in ErrorSubtypes.BUILT_IN_ERRORS }
            .also { if (it.isNotEmpty()) log.info { "Discovered ${it.size} custom GraphQL error subtypes" } }
    }
}
