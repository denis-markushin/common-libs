package org.dema.graphql.dgs.autoconfigure

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.Jackson3JsonProvider
import com.jayway.jsonpath.spi.mapper.Jackson3MappingProvider
import com.netflix.graphql.dgs.json.DgsJsonMapper
import com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration
import org.dema.graphql.dgs.error.ConflictError
import org.dema.graphql.dgs.error.ErrorInterface
import org.dema.graphql.dgs.error.ForbiddenError
import org.dema.graphql.dgs.error.NotFoundError
import org.dema.graphql.dgs.error.RuntimeError
import org.dema.graphql.dgs.error.ServiceUnavailableError
import org.dema.graphql.dgs.error.UnauthorizedError
import org.dema.graphql.dgs.error.ValidationError
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.util.ClassUtils
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.NamedType
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinModule

@AutoConfiguration(before = [DgsSpringGraphQLAutoConfiguration::class])
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

    private fun subtypesModule(): SimpleModule =
        SimpleModule("ErrorInterfaceSubtypes").apply { registerSubtypes(*subtypes) }

    @Bean
    fun errorInterfaceSubtypesModule(): SimpleModule = subtypesModule()

    @Bean
    @ConditionalOnMissingBean(DgsJsonMapper::class)
    fun errorInterfaceDgsJsonMapper(): DgsJsonMapper = ErrorInterfaceDgsJsonMapper(subtypesModule())

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

/**
 * DGS JSON mapper that mirrors the defaults of DGS's own
 * `Jackson3DgsJsonMapper` and additionally knows every [ErrorInterface]
 * subtype, so that `DgsQueryExecutor.executeAndExtractJsonPathAsObject`
 * resolves typed GraphQL errors through their `__typename` discriminator.
 */
private class ErrorInterfaceDgsJsonMapper(
    subtypes: JacksonModule,
) : DgsJsonMapper {
    private val mapper: JsonMapper = JsonMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .addModule(subtypes)
        .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .build()

    override fun writeValueAsString(value: Any): String = mapper.writeValueAsString(value)

    override fun <T> readValue(content: String, clazz: Class<T>): T = mapper.readValue(content, clazz)

    override fun <T> convertValue(fromValue: Any, toClass: Class<T>): T = mapper.convertValue(fromValue, toClass)

    override fun jsonPathConfiguration(): Configuration = Configuration.builder()
        .jsonProvider(Jackson3JsonProvider(mapper))
        .mappingProvider(Jackson3MappingProvider(mapper))
        .build()
        .addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL)
}
