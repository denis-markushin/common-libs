package org.dema.graphql.dgs.loader

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import io.mockk.every
import io.mockk.mockk
import org.dataloader.DataLoaderFactory
import org.dataloader.MappedBatchLoader
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

private class SuffixLoader : MappedBatchLoader<Int, String> {
    override fun load(keys: Set<Int>): CompletionStage<Map<Int, String>> =
        CompletableFuture.completedFuture(keys.associateWith { "value-$it" })
}

class DataLoaderExtensionsTest {

    @Test
    fun `load resolves value through data loader registered for the loader class`() {
        val dataLoader = DataLoaderFactory.newMappedDataLoader(SuffixLoader())
        val dfe = mockk<DgsDataFetchingEnvironment>()
        every { dfe.getDataLoader<Int, String>(SuffixLoader::class.java) } returns dataLoader
        val future = dfe.load(17, SuffixLoader::class)
        dataLoader.dispatch()
        assertThat(future.get(5, TimeUnit.SECONDS), name = "value was not resolved via SuffixLoader").isEqualTo("value-17")
    }
}
