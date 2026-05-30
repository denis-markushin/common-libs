package org.dema.graphql.dgs.autoconfigure

import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.databind.module.SimpleModule
import com.netflix.graphql.dgs.internal.BaseDgsQueryExecutor
import org.dema.graphql.dgs.error.ConflictError
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ForbiddenError
import org.dema.graphql.dgs.error.NotFoundError
import org.dema.graphql.dgs.error.RuntimeError
import org.dema.graphql.dgs.error.ServiceUnavailableError
import org.dema.graphql.dgs.error.UnauthorizedError
import org.dema.graphql.dgs.error.ValidationError
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.util.ClassUtils

@AutoConfiguration
class ErrorInterfaceJacksonAutoConfiguration(
    private val context: ApplicationContext,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val subtypes: Array<NamedType> = (BUILT_IN_ERRORS + discoverCustomErrors())
        .map { NamedType(it, it.simpleName) }
        .toTypedArray()

    private fun discoverCustomErrors(): List<Class<*>> {
        val packages = runCatching { AutoConfigurationPackages.get(context) }
            .getOrNull()
            .orEmpty()
        if (packages.isEmpty()) return emptyList()

        val scanner = ClassPathScanningCandidateComponentProvider(false).apply {
            addIncludeFilter(AssignableTypeFilter(ErrorInterface::class.java))
            setResourceLoader(context)
        }

        val classLoader = context.classLoader ?: ClassUtils.getDefaultClassLoader()

        return packages
            .flatMap { scanner.findCandidateComponents(it) }
            .mapNotNull { candidate ->
                candidate.beanClassName?.let { className ->
                    runCatching { ClassUtils.forName(className, classLoader) }
                        .onFailure { log.warn("Failed to load error class: {}", className, it) }
                        .getOrNull()
                }
            }
            .filterNot { it in BUILT_IN_ERRORS }
            .also { log.info("Discovered {} custom GraphQL error subtypes", it.size) }
    }

    @Bean
    fun errorInterfaceSubtypesModule(): SimpleModule =
        SimpleModule("ErrorInterfaceSubtypes").apply { registerSubtypes(*subtypes) }

    @Bean
    fun errorInterfaceDgsMapperRegistrar(): SmartInitializingSingleton =
        SmartInitializingSingleton {
            log.debug("Registering {} error subtypes into DGS static ObjectMapper", subtypes.size)
            BaseDgsQueryExecutor.objectMapper.registerSubtypes(*subtypes)
        }

    private companion object {
        val BUILT_IN_ERRORS = setOf(
            NotFoundError::class.java,
            ValidationError::class.java,
            ConflictError::class.java,
            UnauthorizedError::class.java,
            ForbiddenError::class.java,
            ServiceUnavailableError::class.java,
            RuntimeError::class.java,
        )
    }
}
