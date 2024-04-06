package io.github.moreirasantos.pgkn

import io.github.moreirasantos.pgkn.paramsource.MapSqlParameterSource
import io.github.moreirasantos.pgkn.paramsource.SqlParameterSource
import io.github.moreirasantos.pgkn.resultset.PostgresResultSet
import io.github.moreirasantos.pgkn.resultset.ResultSet
import io.r2dbc.pool.PoolingConnectionFactoryProvider.MAX_SIZE
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions.*
import io.r2dbc.spi.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import java.util.*


@Suppress("LongParameterList")
suspend fun PostgresDriver(
    host: String,
    port: Int = 5432,
    database: String,
    user: String,
    password: String,
    poolSize: Int = 20
): PostgresDriver {


    val connectionFactory = ConnectionFactories.get(
        builder()
            .option(DRIVER, "pool")
            .option(PROTOCOL, "postgresql")
            .option(HOST, host)
            .option(PORT, port)
            .option(USER, user)
            .option(PASSWORD, password)
            .option(DATABASE, database)
            .option(MAX_SIZE, poolSize)
            .build()
    )

    val connection = connectionFactory.create().awaitFirst()

    return PostgresDriverPool(connection = connection)
}

private class PostgresDriverPool(private val connection: Connection) : PostgresDriver {
    override suspend fun <T> execute(sql: String, namedParameters: Map<String, Any?>, handler: (ResultSet) -> T) =
        if (namedParameters.isEmpty()) doExecute(sql).handleResults(handler)
        else execute(sql, MapSqlParameterSource(namedParameters), handler)

    override suspend fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T) =
        doExecute(sql, paramSource).handleResults(handler)

    override suspend fun execute(sql: String, namedParameters: Map<String, Any?>): Long =
        if (namedParameters.isEmpty()) doExecute(sql).returnCount()
        else execute(sql, MapSqlParameterSource(namedParameters))

    override suspend fun execute(sql: String, paramSource: SqlParameterSource): Long =
        doExecute(sql, paramSource).returnCount()

    private fun doExecute(sql: String): Flow<Result> {
        return connection.createStatement(sql).execute().asFlow()
    }

    private fun doExecute(sql: String, paramSource: SqlParameterSource) = (paramSource.parameterNames ?: emptyArray())
            .fold(connection.createStatement(sql)) { acc, name ->
                paramSource.getValue(name)
                    ?.let { acc.bind(name, it) }
                    ?: acc.bindNull(name, Any::class.java)
            }
            .execute()
            .asFlow()

    // Await First and toList both suspend?
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> Flow<Result>.handleResults(handler: (ResultSet) -> T) = flatMapConcat {
        return@flatMapConcat it.map { row -> Optional.ofNullable(handler(PostgresResultSet(row))) }.asFlow()
    }.map { it.orElse(null) }.toList()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun Flow<Result>.returnCount() = flatMapConcat {
        it.rowsUpdated.asFlow()
    }.fold(0L) { accumulator, value -> accumulator + value }

}
