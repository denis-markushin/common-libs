package org.dema.test

import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Runs one action from several threads at once to provoke a race in a test.
 * The threads act as the user of the test: services that journal access require authentication.
 */
object Race {
    fun <T> run(threads: Int, action: (Int) -> T): List<T> {
        val pool: ExecutorService = DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(threads))
        val barrier = CyclicBarrier(threads)
        try {
            val tasks = (0 until threads).map { index -> Callable { barrier.await(30, TimeUnit.SECONDS); action(index) } }
            return pool.invokeAll(tasks, 30, TimeUnit.SECONDS).map { it.get() }
        } finally {
            pool.shutdownNow()
        }
    }
}
