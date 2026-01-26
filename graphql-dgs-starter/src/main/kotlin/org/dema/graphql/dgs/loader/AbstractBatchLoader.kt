package org.dema.graphql.dgs.loader

import org.dataloader.MappedBatchLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executor

/**
 * Lightweight base class for DGS/DataLoader batch loading that delegates work to a supplied function
 * and executes it asynchronously on a provided [Executor].
 *
 * This implementation:
 * - Uses DataLoader's mapped style ([MappedBatchLoader]) — the loader function must return a map of found values.
 * - Returns an empty map immediately for empty input (avoids scheduling overhead).
 * - Does not impose ordering; ordering is handled by DataLoader when mapping results back to requested keys.
 *
 * ### Conventions
 * - **Missing keys** MUST be omitted from the returned map (do not insert null values).
 * - The returned map SHOULD be immutable externally (prefer Kotlin immutable maps).
 *
 * @param K key type (e.g., UUID)
 * @param V value type (domain object)
 * @param executor executor used to run the batch function asynchronously
 * @param loaderFn function that receives a set of keys and returns a map of found values (keys not found must be absent)
 */
abstract class AbstractBatchLoader<K, V>(
    private val executor: Executor,
    private val loaderFn: (Set<K>) -> Map<K, V>,
) : MappedBatchLoader<K, V> {

    /**
     * Loads values for the given [keys] asynchronously.
     *
     * - Returns a completed future with an empty map if [keys] is empty.
     * - Otherwise schedules [loaderFn] on [executor].
     *
     * @param keys set of keys to load
     * @return a [CompletionStage] that completes with a map of found values
     */
    override fun load(keys: Set<K>): CompletionStage<Map<K, V>> =
        if (keys.isEmpty()) {
            CompletableFuture.completedFuture(emptyMap())
        } else {
            CompletableFuture.supplyAsync({ loaderFn(keys) }, executor)
        }
}
