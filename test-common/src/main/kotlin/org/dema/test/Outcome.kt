package org.dema.test

import org.dema.graphql.dgs.error.exception.ConflictException

/**
 * Reduces a call to a line a test can compare: SUCCESS, the reason of a conflict or the class name
 * of an unexpected exception. A failed call needs graphql-dgs-starter on the test classpath,
 * otherwise it ends in NoClassDefFoundError for ConflictException.
 */
object Outcome {
    fun of(action: () -> Any?): String =
        runCatching { action() }.fold({ "SUCCESS" }, { (it as? ConflictException)?.reason ?: it.javaClass.simpleName })
}
