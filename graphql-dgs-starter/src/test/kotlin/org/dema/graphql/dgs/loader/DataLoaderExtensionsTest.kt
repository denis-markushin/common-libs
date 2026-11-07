package org.dema.graphql.dgs.loader

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import com.netflix.graphql.dgs.DgsDataLoader
import graphql.schema.DataFetchingEnvironmentImpl
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderRegistry
import org.dataloader.MappedBatchLoader
import org.junit.jupiter.api.Test
import org.springframework.context.support.StaticApplicationContext
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fake loader that answers each key with a string carrying it, so a value proves it came through the DataLoader,
 * and counts its calls. The registry knows it by its simple name, the way `@DgsDataLoader` without a name registers it.
 */
@DgsDataLoader
private class EchoLoader(
    private val calls: AtomicInteger,
) : MappedBatchLoader<UUID, String> {
    override fun load(keys: Set<UUID>): CompletionStage<Map<UUID, String>> {
        calls.incrementAndGet()
        return CompletableFuture.completedFuture(keys.associateWith { "echo-$it" })
    }
}

private fun environment(registry: DataLoaderRegistry): DgsDataFetchingEnvironment =
    DgsDataFetchingEnvironment(
        DataFetchingEnvironmentImpl.newDataFetchingEnvironment().dataLoaderRegistry(registry).build(),
        StaticApplicationContext(),
    )

class DataLoaderExtensionsTest {

    @Test
    fun `load resolves value through data loader registered for the loader class`() {
        val key = UUID.randomUUID()
        val registry = DataLoaderRegistry().register("EchoLoader", DataLoaderFactory.newMappedDataLoader(EchoLoader(AtomicInteger())))
        val future = environment(registry).load(key, EchoLoader::class)
        registry.dispatchAll()
        assertThat(future.get(5, TimeUnit.SECONDS), name = "value was not resolved via EchoLoader").isEqualTo("echo-$key")
    }

    @Test
    fun `load or null of an absent key completes with null`() {
        val registry = DataLoaderRegistry().register("EchoLoader", DataLoaderFactory.newMappedDataLoader(EchoLoader(AtomicInteger())))
        val future = environment(registry).loadOrNull(null, EchoLoader::class)
        assertThat(future.get(5, TimeUnit.SECONDS), name = "absent key produced a value").isNull()
    }

    @Test
    fun `load or null of an absent key never asks the loader`() {
        val calls = AtomicInteger()
        val registry = DataLoaderRegistry().register("EchoLoader", DataLoaderFactory.newMappedDataLoader(EchoLoader(calls)))
        environment(registry).loadOrNull(null, EchoLoader::class)
        registry.dispatchAll()
        assertThat(calls.get(), name = "loader was asked for an absent key").isEqualTo(0)
    }

    @Test
    fun `load or null of a present key resolves the value through its loader`() {
        val key = UUID.randomUUID()
        val registry = DataLoaderRegistry().register("EchoLoader", DataLoaderFactory.newMappedDataLoader(EchoLoader(AtomicInteger())))
        val future = environment(registry).loadOrNull(key, EchoLoader::class)
        registry.dispatchAll()
        assertThat(future.get(5, TimeUnit.SECONDS), name = "value did not come through the loader of its class").isEqualTo("echo-$key")
    }
}
