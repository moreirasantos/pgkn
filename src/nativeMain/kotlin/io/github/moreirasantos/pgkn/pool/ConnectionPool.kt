package io.github.moreirasantos.pgkn.pool

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Pool with Semaphore/Mutex pattern.
 * Using semaphore with max permits being the size of connection pool to throttle.
 * Mutex to make sure one connection is not used at the same time-
 */
internal class ConnectionPool<T>(connections: List<T>) {

    private val mutex = Mutex()
    private val semaphore = Semaphore(permits = connections.size)
    private val connections = connections.toMutableList()

    suspend operator fun <U> invoke(handler: suspend (T) -> U): U {
        semaphore.withPermit {
            val borrowed = mutex.withLock { connections.removeLast() }
            try {
                return handler(borrowed)
            } finally {
                mutex.withLock { connections.add(borrowed) }
            }
        }
    }
}
