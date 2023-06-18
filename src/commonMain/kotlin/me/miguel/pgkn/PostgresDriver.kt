package me.miguel.pgkn

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.createValues
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import libpq.*
import me.miguel.pgkn.resultset.PostgresResultSet
import me.miguel.pgkn.resultset.ResultSet

sealed interface PostgresDriver {
    fun <T> executeQuery(sql: String, handler: (ResultSet) -> T): List<T>
}

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

    override fun <T> executeQuery(sql: String, handler: (ResultSet) -> T): List<T> = memScoped {
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
        .let {
            val rs = PostgresResultSet(it)

            val list: MutableList<T> = mutableListOf()
            while (rs.next()) {
                list.add(handler(rs))
            }

            PQclear(it)
            return list
        }

    private fun CPointer<PGresult>?.check(): CPointer<PGresult> {
        val status = PQresultStatus(this)
        check(status == PGRES_TUPLES_OK || status == PGRES_COMMAND_OK || status == PGRES_COPY_IN) {
            connection.error()
        }
        return this!!
    }
}

private fun CPointer<PGconn>?.error(): String = PQerrorMessage(this)!!.toKString().also { PQfinish(this) }

private const val TEXT_RESULT_FORMAT = 0
private const val BINARY_RESULT_FORMAT = 1