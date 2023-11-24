package io.github.moreirasantos.pgkn

import io.github.moreirasantos.pgkn.paramsource.MapSqlParameterSource
import io.github.moreirasantos.pgkn.paramsource.SqlParameterSource
import io.github.moreirasantos.pgkn.pool.ConnectionPool
import io.github.moreirasantos.pgkn.resultset.PostgresResultSet
import io.github.moreirasantos.pgkn.resultset.ResultSet
import io.github.moreirasantos.pgkn.sql.buildValueArray
import io.github.moreirasantos.pgkn.sql.parseSql
import io.github.moreirasantos.pgkn.sql.substituteNamedParameters
import kotlinx.cinterop.*
import libpq.*

/**
 * Executes given query with given named parameters.
 * If you pass a handler, you will receive a list of result data.
 * You can pass an [SqlParameterSource] to register your own Postgres types.
 */
sealed interface PostgresDriver {
    suspend fun <T> execute(
        sql: String,
        namedParameters: Map<String, Any?> = emptyMap(),
        handler: (ResultSet) -> T
    ): List<T>

    suspend fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T): List<T>
    suspend fun execute(sql: String, namedParameters: Map<String, Any?> = emptyMap()): Long
    suspend fun execute(sql: String, paramSource: SqlParameterSource): Long
}

sealed interface PostgresDriverUnit {
    fun <T> execute(
        sql: String,
        namedParameters: Map<String, Any?> = emptyMap(),
        handler: (ResultSet) -> T
    ): List<T>

    fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T): List<T>
    fun execute(sql: String, namedParameters: Map<String, Any?> = emptyMap()): Long
    fun execute(sql: String, paramSource: SqlParameterSource): Long
}

@Suppress("LongParameterList")
@OptIn(ExperimentalForeignApi::class)
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

@OptIn(ExperimentalForeignApi::class)
fun PostgresDriverUnit(
    host: String,
    port: Int = 5432,
    database: String,
    user: String,
    password: String
): PostgresDriverUnit = PostgresDriverImpl(
    host = host,
    port = port,
    database = database,
    user = user,
    password = password
)

@ExperimentalForeignApi
private class PostgresDriverPool(
    host: String,
    port: Int = 5432,
    database: String,
    user: String,
    password: String,
    poolSize: Int
) : PostgresDriver {

    private val pool = ConnectionPool((1..poolSize).map {
        PostgresDriverImpl(
            host = host,
            port = port,
            database = database,
            user = user,
            password = password,
        )
    })

    override suspend fun <T> execute(
        sql: String,
        namedParameters: Map<String, Any?>,
        handler: (ResultSet) -> T
    ) = pool.invoke { it.execute(sql, namedParameters, handler) }

    override suspend fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T) =
        pool.invoke { it.execute(sql, paramSource, handler) }

    override suspend fun execute(sql: String, namedParameters: Map<String, Any?>) =
        pool.invoke { it.execute(sql, namedParameters) }

    override suspend fun execute(sql: String, paramSource: SqlParameterSource) =
        pool.invoke { it.execute(sql, paramSource) }
}

@ExperimentalForeignApi
private class PostgresDriverImpl(
    private val host: String,
    private val port: Int,
    private val database: String,
    private val user: String,
    private val password: String,
) : PostgresDriverUnit {

    var connection = initConnection()

    private fun initConnection() = PQsetdbLogin(
        pghost = host,
        pgport = port.toString(),
        dbName = database,
        login = user,
        pwd = password,
        pgoptions = null,
        pgtty = null
    ).apply { require(ConnStatusType.CONNECTION_OK == PQstatus(this)) }!!

    override fun <T> execute(sql: String, namedParameters: Map<String, Any?>, handler: (ResultSet) -> T) =
        if (namedParameters.isEmpty()) doExecute(sql).handleResults(handler)
        else execute(sql, MapSqlParameterSource(namedParameters), handler)

    override fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T) =
        doExecute(sql, paramSource).handleResults(handler)

    override fun execute(sql: String, namedParameters: Map<String, Any?>) =
        if (namedParameters.isEmpty()) doExecute(sql).returnCount()
        else execute(sql, MapSqlParameterSource(namedParameters))

    override fun execute(sql: String, paramSource: SqlParameterSource) =
        doExecute(sql, paramSource).returnCount()

    private fun <T> CPointer<PGresult>.handleResults(handler: (ResultSet) -> T): List<T> {
        val rs = PostgresResultSet(this)

        val list: MutableList<T> = mutableListOf()
        while (rs.next()) {
            list.add(handler(rs))
        }

        PQclear(this)
        return list
    }

    private fun CPointer<PGresult>.returnCount(): Long {
        val rows = PQcmdTuples(this)!!.toKString()
        PQclear(this)
        return rows.toLongOrNull() ?: 0
    }

    private fun doExecute(sql: String, paramSource: SqlParameterSource): CPointer<PGresult> {
        val parsedSql = parseSql(sql)
        val sqlToUse: String = substituteNamedParameters(parsedSql, paramSource)
        val params: Array<Any?> = buildValueArray(parsedSql, paramSource)

        return memScoped {
            PQexecParams(
                connection,
                command = sqlToUse,
                nParams = params.size,
                paramValues = createValues(params.size) {
                    println(params[it]?.toString()?.cstr)
                    value = params[it]?.toString()?.cstr?.getPointer(this@memScoped)
                },
                paramLengths = params.map { it?.toString()?.length ?: 0 }.toIntArray().refTo(0),
                paramFormats = IntArray(params.size) { TEXT_RESULT_FORMAT }.refTo(0),
                paramTypes = parsedSql.parameterNames.map(paramSource::getSqlType).toUIntArray().refTo(0),
                resultFormat = TEXT_RESULT_FORMAT
            )
        }.check()
    }

    private fun doExecute(sql: String) = memScoped {
        PQexecParams(
            connection,
            command = sql,
            nParams = 0,
            paramValues = createValues(0) {},
            paramLengths = createValues(0) {},
            paramFormats = createValues(0) {},
            paramTypes = createValues(0) {},
            resultFormat = TEXT_RESULT_FORMAT
        )
    }.check()

    private fun CPointer<PGresult>?.check(): CPointer<PGresult> {
        val status = PQresultStatus(this)
        check(status == PGRES_TUPLES_OK || status == PGRES_COMMAND_OK || status == PGRES_COPY_IN) {
            val message = connection.error()
            if (status == PGRES_FATAL_ERROR) {
                PQfinish(connection)
                connection = initConnection()
            }
            message
        }
        return this!!
    }
}

@ExperimentalForeignApi
private fun CPointer<PGconn>?.error(): String = PQerrorMessage(this)!!.toKString()

private const val TEXT_RESULT_FORMAT = 0

@Suppress("UnusedPrivateProperty")
private const val BINARY_RESULT_FORMAT = 1
