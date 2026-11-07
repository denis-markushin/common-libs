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

/**
 * Resolves an optional relation through the DataLoader registered for the given [loader] class.
 *
 * An absent [key] completes at once with `null` and never reaches the DataLoader; a present one is loaded
 * in the batch like [load]. The future itself is never `null`, the absence lives inside it: a field returning
 * a nullable future is wrapped twice by DGS on virtual threads, and every relation of the field resolves to `null`.
 *
 * @param K key type (e.g., UUID)
 * @param V value type (domain object)
 * @param key key of the relation, `null` when the relation is absent
 * @param loader class of the DGS-registered [MappedBatchLoader] to delegate to
 * @return a [CompletableFuture] completing with the loaded value, or with `null` for an absent key
 */
fun <K : Any, V : Any> DgsDataFetchingEnvironment.loadOrNull(
    key: K?,
    loader: KClass<out MappedBatchLoader<K, V>>,
): CompletableFuture<V?> = if (key == null) CompletableFuture.completedFuture(null) else load(key, loader).thenApply { it }
