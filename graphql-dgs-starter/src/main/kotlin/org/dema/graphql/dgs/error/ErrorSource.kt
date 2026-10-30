package org.dema.graphql.dgs.error

/**
 * An exception that knows the typed GraphQL error it answers the client with: a service declares its error
 * type where it throws the exception, with no mapper of its own and no mapper order to work out.
 */
interface ErrorSource {
    val error: ErrorInterface
}
