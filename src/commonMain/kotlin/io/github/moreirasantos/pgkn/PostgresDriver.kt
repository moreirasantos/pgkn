package io.github.moreirasantos.pgkn

import kotlinx.cinterop.*
import libpq.*
import io.github.moreirasantos.pgkn.resultset.PostgresResultSet
import io.github.moreirasantos.pgkn.resultset.ResultSet

sealed interface PostgresDriver {
    fun <T> execute(sql: String, handler: (ResultSet) -> T): List<T>

    fun execute(sql: String): Long
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

    override fun execute(sql: String): Long = doExecute(sql).let {
        val rows = PQcmdTuples(it)!!.toKString()
        PQclear(it)
        return rows.toLongOrNull() ?: 0
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
    }
        .check()

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
