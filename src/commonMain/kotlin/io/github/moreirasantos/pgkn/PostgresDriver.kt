package io.github.moreirasantos.pgkn

import io.github.moreirasantos.pgkn.paramsource.MapSqlParameterSource
import io.github.moreirasantos.pgkn.paramsource.SqlParameterSource
import io.github.moreirasantos.pgkn.resultset.PostgresResultSet
import io.github.moreirasantos.pgkn.resultset.ResultSet
import io.github.moreirasantos.pgkn.sql.buildValueArray
import io.github.moreirasantos.pgkn.sql.parseSql
import io.github.moreirasantos.pgkn.sql.substituteNamedParameters
import kotlinx.cinterop.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import libpq.*

sealed interface PostgresDriver {
    fun <T> execute(sql: String, handler: (ResultSet) -> T): List<T>
    fun <T> execute(sql: String, namedParameters: Map<String, Any?>, handler: (ResultSet) -> T): List<T>

    fun execute(sql: String): Long
    fun execute(sql: String, namedParameters: Map<String, Any?>): Long
}

@OptIn(ExperimentalForeignApi::class)
fun PostgresDriver(
    host: String,
    port: Int = 5432,
    database: String,
    user: String,
    password: String,
): PostgresDriver = PostgresDriverImpl(
    host = host,
    port = port,
    database = database,
    user = user,
    password = password
)

@ExperimentalForeignApi
private class PostgresDriverImpl(
    host: String,
    port: Int,
    database: String,
    user: String,
    password: String,
) : PostgresDriver {

    private val connection = PQsetdbLogin(
        pghost = host,
        pgport = port.toString(),
        dbName = database,
        login = user,
        pwd = password,
        pgoptions = null,
        pgtty = null
    ).apply { require(ConnStatusType.CONNECTION_OK == PQstatus(this)) }!!

    override fun <T> execute(sql: String, handler: (ResultSet) -> T): List<T> = doExecute(sql).let {
        val rs = PostgresResultSet(it)

        val list: MutableList<T> = mutableListOf()
        while (rs.next()) {
            list.add(handler(rs))
        }

        PQclear(it)
        return list
    }

    override fun <T> execute(sql: String, namedParameters: Map<String, Any?>, handler: (ResultSet) -> T): List<T> =
        execute(sql, MapSqlParameterSource(namedParameters), handler)

    fun <T> execute(sql: String, paramSource: SqlParameterSource, handler: (ResultSet) -> T): List<T> =
        doExecute(sql, paramSource)
            .let {
                val rs = PostgresResultSet(it)

                val list: MutableList<T> = mutableListOf()
                while (rs.next()) {
                    list.add(handler(rs))
                }

                PQclear(it)
                list
            }

    override fun execute(sql: String): Long = doExecute(sql).let {
        val rows = PQcmdTuples(it)!!.toKString()
        PQclear(it)
        return rows.toLongOrNull() ?: 0
    }

    override fun execute(sql: String, namedParameters: Map<String, Any?>) =
        execute(sql, MapSqlParameterSource(namedParameters))

    fun execute(sql: String, paramSource: SqlParameterSource): Long = doExecute(sql, paramSource)
        .let {
            val rows = PQcmdTuples(it)!!.toKString()
            PQclear(it)
            rows.toLongOrNull() ?: 0
        }

    private fun doExecute(sql: String, paramSource: SqlParameterSource): CPointer<PGresult> {
        val parsedSql = parseSql(sql)
        val sqlToUse: String = substituteNamedParameters(parsedSql, paramSource)
        // We may not need this since we could infer type from class name?
        // val declaredParameters: List<SqlParameter> = buildSqlParameterList(parsedSql, paramSource)
        val params: Array<Any?> = buildValueArray(parsedSql, paramSource, null)

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
                paramTypes = params.map {
                    oidMap[it!!::class.simpleName]!!/*it.sqlType.toUInt()*/
                }.toUIntArray().refTo(0),
                resultFormat = TEXT_RESULT_FORMAT
            )
        }.check()
    }

    @Suppress("LongParameterList")
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
            connection.error()
        }
        return this!!
    }
}

@ExperimentalForeignApi
private fun CPointer<PGconn>?.error(): String = PQerrorMessage(this)!!.toKString().also { PQfinish(this) }

private const val TEXT_RESULT_FORMAT = 0

@Suppress("UnusedPrivateProperty")
private const val BINARY_RESULT_FORMAT = 1


private val oidMap: Map<String?, UInt> = hashMapOf(
    Boolean::class.simpleName to 16u,
    ByteArray::class.simpleName to 17u,
    Long::class.simpleName to 20u,
    Int::class.simpleName to 20u, // TODO this is long not int
    String::class.simpleName to 25u,
    Double::class.simpleName to 701u,
    LocalDate::class.simpleName to 1082u,
    LocalTime::class.simpleName to 1083u,
    // intervalOid = 1186u
    LocalDateTime::class.simpleName to 1114u,
    Instant::class.simpleName to 1184u,
    // uuidOid = 2950u
).withDefault { throw IllegalArgumentException("Wrong Type $it") }
