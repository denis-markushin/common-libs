package org.dema.graphql.dgs.loader

import com.netflix.graphql.dgs.DgsDataFetchingEnvironment
import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

/**
 * Resolves a single value through the DataLoader registered for the given [loader] class.
 *
 * Collapses the recurring federation entity-fetcher body
 * (`getDataLoader(...).load(key)`) into a one-liner; the value type [V]
 * is inferred by the compiler from the loader's [MappedBatchLoader] supertype.
 *
 * @param K key type (e.g., UUID)
 * @param V value type (domain object)
 * @param key key to resolve
 * @param loader class of the DGS-registered [MappedBatchLoader] to delegate to
 * @return a [CompletableFuture] completing with the loaded value, or `null` entry semantics per DataLoader
 */
fun <K : Any, V : Any> DgsDataFetchingEnvironment.load(
    key: K,
    loader: KClass<out MappedBatchLoader<K, V>>,
): CompletableFuture<V> = getDataLoader<K, V>(loader.java).load(key)
