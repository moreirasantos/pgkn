package io.github.moreirasantos.pgkn

import io.github.moreirasantos.pgkn.paramsource.SqlParameterSource
import io.github.moreirasantos.pgkn.resultset.ResultSet

/**
 * Executes given query with given named parameters.
 * If you pass a handler, you will receive a list of result data.
 * You can pass an [SqlParameterSource] to register your own Postgres types.
 */
interface PostgresDriver {
    suspend fun <T> execute(
        sql: String,
        namedParameters: Map<String, Any?> = emptyMap(),
        handler: (ResultSet) -> T
    ): List<T>

    suspend fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T): List<T>
    suspend fun execute(sql: String, namedParameters: Map<String, Any?> = emptyMap()): Long
    suspend fun execute(sql: String, paramSource: SqlParameterSource): Long

    /**
     * Warm-up the connection pool.
     */
    suspend fun warmup()
}
