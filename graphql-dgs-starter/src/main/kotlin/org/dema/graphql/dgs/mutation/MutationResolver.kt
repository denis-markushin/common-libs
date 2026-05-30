package org.dema.graphql.dgs.mutation

import graphql.schema.DataFetchingEnvironment

/**
 * Provides the `DataFetchingEnvironment.resolveMutation { ... }` extension
 * used by `@DgsComponent` mutation classes.
 *
 * Consumers inject this interface and bring the extension into scope via
 * Kotlin's interface delegation, preserving the DSL at the call site:
 *
 * ```kotlin
 * @DgsComponent
 * class ProjectMutations(
 *     mutationResolver: MutationResolver,
 *     private val projectService: ProjectService,
 * ) : MutationResolver by mutationResolver {
 *     fun create(input: CreateProjectInput, dfe: DataFetchingEnvironment) =
 *         when (val r = dfe.resolveMutation { projectService.create(input) }) {
 *             is MutationOutcome.Success -> ...
 *             is MutationOutcome.Failure -> ...
 *         }
 * }
 * ```
 */
interface MutationResolver {
    fun <T> DataFetchingEnvironment.resolveMutation(block: () -> T): MutationOutcome<T>
}
