package io.github.moreirasantos.pgkn

import io.github.moreirasantos.pgkn.paramsource.MapSqlParameterSource
import io.github.moreirasantos.pgkn.paramsource.SqlParameterSource
import io.github.moreirasantos.pgkn.resultset.PostgresResultSet
import io.github.moreirasantos.pgkn.resultset.ResultSet
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.PoolingConnectionFactoryProvider.INITIAL_SIZE
import io.r2dbc.pool.PoolingConnectionFactoryProvider.MAX_SIZE
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.PROTOCOL
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import io.r2dbc.spi.ConnectionFactoryOptions.builder
import io.r2dbc.spi.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import java.util.*

@Suppress("LongParameterList")
fun PostgresDriver(
    host: String,
    port: Int = 5432,
    database: String,
    user: String,
    password: String,
    poolSize: Int = 20
): PostgresDriver = PostgresDriverPool(
    host = host,
    port = port,
    database = database,
    user = user,
    password = password,
    poolSize = poolSize
)

private class PostgresDriverPool(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String,
    poolSize: Int
) : PostgresDriver {

    // https://github.com/r2dbc/r2dbc-pool
    private val pool: ConnectionFactory = ConnectionFactories.get(
        builder()
            .option(DRIVER, "pool")
            .option(PROTOCOL, "postgresql")
            .option(HOST, host)
            .option(PORT, port)
            .option(USER, user)
            .option(PASSWORD, password)
            .option(DATABASE, database)
            .option(INITIAL_SIZE, poolSize)
            .option(MAX_SIZE, poolSize)
            .build()
    )

    override suspend fun <T> execute(
        sql: String,
        namedParameters: Map<String, Any?>,
        handler: (ResultSet) -> T
    ): List<T> =
        if (namedParameters.isEmpty()) doExecute(sql).handleResults(handler)
        else execute(sql, MapSqlParameterSource(namedParameters), handler)

    override suspend fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T): List<T> =
        doExecute(sql, paramSource).handleResults(handler)

    override suspend fun execute(sql: String, namedParameters: Map<String, Any?>): Long =
        if (namedParameters.isEmpty()) doExecute(sql).returnCount()
        else execute(sql, MapSqlParameterSource(namedParameters))

    override suspend fun execute(sql: String, paramSource: SqlParameterSource): Long =
        doExecute(sql, paramSource).returnCount()

    override suspend fun warmup() {
        // ConnectionFactories.get(..) creates a [ConnectionPool] wrapping an underlying [ConnectionFactory].
        (pool as ConnectionPool).warmup().awaitFirst()
    }

    private suspend fun doExecute(sql: String): Flow<Result> =
        pool.create().awaitFirst().createStatement(sql).execute().asFlow()

    private suspend fun doExecute(sql: String, paramSource: SqlParameterSource): Flow<Result> =
        (paramSource.parameterNames ?: emptyArray()).fold(
            pool.create().awaitFirst().createStatement(sql)
        ) { acc, name ->
            paramSource.getValue(name)?.let { acc.bind(name, it) }
                ?: acc.bindNull(name, Any::class.java)
        }.execute().asFlow()

    // Await First and toList both suspend?
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun <T> Flow<Result>.handleResults(handler: (ResultSet) -> T): List<T> =
        flatMapConcat { it.map { row -> Optional.ofNullable(handler(PostgresResultSet(row))) }.asFlow() }
            .map { it.orElse(null) }.toList()

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun Flow<Result>.returnCount(): Long =
        flatMapConcat { it.rowsUpdated.asFlow() }
            .fold(0L) { accumulator, value -> accumulator + value }
}
