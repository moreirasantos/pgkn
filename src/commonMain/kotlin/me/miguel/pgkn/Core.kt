package me.miguel.pgkn

import kotlinx.cinterop.*
import kotlinx.datetime.*
import libpq.*

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

sealed interface PostgresDriver {
    fun <T> executeQuery(sql: String, handler: (ResultSet) -> T): List<T>
}

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


private class PostgresResultSet(val internal: CPointer<PGresult>) : ResultSet {

    private val rowCount: Int = PQntuples(internal)
    private val columnCount: Int = PQnfields(internal)

    private var currentRow = -1


    override fun next(): Boolean {
        if (currentRow > rowCount - 2) {
            return false
        }
        currentRow++
        return true
    }

    private fun isNull(columnIndex: Int): Boolean =
        PQgetisnull(res = internal, tup_num = currentRow, field_num = columnIndex) == 1

    private fun getPointer(columnIndex: Int): CPointer<ByteVar>? {
        if (isNull(columnIndex)) return null
        return PQgetvalue(res = internal, tup_num = currentRow, field_num = columnIndex) ?: throw SQLException()
    }


    /**
     * Are all non-binary columns returned as text?
     * https://www.postgresql.org/docs/9.5/libpq-exec.html#LIBPQ-EXEC-SELECT-INFO
     */
    override fun getString(columnIndex: Int): String? = getPointer(columnIndex)?.toKString().also { println(it) }

    override fun getBoolean(columnIndex: Int): Boolean? = getString(columnIndex)?.equals("t")

    override fun getShort(columnIndex: Int): Short? = getString(columnIndex)?.toShort()


    override fun getInt(columnIndex: Int): Int? = getString(columnIndex)?.toInt()


    override fun getLong(columnIndex: Int): Long? = getString(columnIndex)?.toLong()


    override fun getFloat(columnIndex: Int): Float? = getString(columnIndex)?.toFloat()

    override fun getDouble(columnIndex: Int): Double? = getString(columnIndex)?.toDouble()

    override fun getBytes(columnIndex: Int): ByteArray? = getString(columnIndex)?.encodeToByteArray()

    override fun getDate(columnIndex: Int): LocalDate? = getString(columnIndex)?.toLocalDate()

    override fun getTime(columnIndex: Int): LocalTime? = getString(columnIndex)?.toLocalTime()
    override fun getLocalDateTime(columnIndex: Int): LocalDateTime? = getString(columnIndex)
        ?.fixIso8601()
        ?.toLocalDateTime()

    override fun getInstant(columnIndex: Int): Instant? = getString(columnIndex)
        ?.fixIso8601()
        ?.toInstant()
}


/**
 * To Fix ISO 8601, as postgres default is space not "T"
 * https://www.postgresql.org/docs/current/datatype-datetime.html#DATATYPE-DATETIME-OUTPUT
 */
private fun String.fixIso8601() = replaceRange(10, 11, "T")

private fun CPointer<PGconn>?.error(): String = PQerrorMessage(this)!!.toKString().also { PQfinish(this) }

private const val TEXT_RESULT_FORMAT = 0
private const val BINARY_RESULT_FORMAT = 1

class SQLException : Exception()
sealed interface ResultSet {

    /**
     * Moves the cursor forward one row from its current position.
     * A {@code ResultSet} cursor is initially positioned
     * before the first row; the first call to the method
     * {@code next} makes the first row the current row; the
     * second call makes the second row the current row, and so on.
     * <p>
     * When a call to the {@code next} method returns {@code false},
     * the cursor is positioned after the last row. Any
     * invocation of a {@code ResultSet} method which requires a
     * current row will result in a {@code SQLException} being thrown.
     *  If the result set type is {@code TYPE_FORWARD_ONLY}, it is vendor specified
     * whether their JDBC driver implementation will return {@code false} or
     *  throw an {@code SQLException} on a
     * subsequent call to {@code next}.
     *
     * <P>If an input stream is open for the current row, a call
     * to the method {@code next} will
     * implicitly close it. A {@code ResultSet} object's
     * warning chain is cleared when a new row is read.
     *
     * @return {@code true} if the new current row is valid;
     * {@code false} if there are no more rows
     */
    fun next(): Boolean


    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `String` in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `null`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getString(columnIndex: Int): String?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Boolean` in the Kotlin programming language.
     *
     * <P>If the designated column has a datatype of CHAR or VARCHAR
     * and contains a "0" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
     * and contains  a 0, a value of `false` is returned.  If the designated column has a datatype
     * of CHAR or VARCHAR
     * and contains a "1" or has a datatype of BIT, TINYINT, SMALLINT, INTEGER or BIGINT
     * and contains  a 1, a value of `true` is returned.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `false`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
    </P> */

    fun getBoolean(columnIndex: Int): Boolean?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Short` in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `0`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getShort(columnIndex: Int): Short?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * an `Int` in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `0`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getInt(columnIndex: Int): Int?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Long` in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `0`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getLong(columnIndex: Int): Long?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Float` in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `0`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getFloat(columnIndex: Int): Float?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Double` in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `0`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getDouble(columnIndex: Int): Double?


    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Byte` array in the Kotlin programming language.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `null`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getBytes(columnIndex: Int): ByteArray?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `LocalDate` object in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `null`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getDate(columnIndex: Int): LocalDate?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `LocalTime` object in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `null`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getTime(columnIndex: Int): LocalTime?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Instant` object in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `null`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getLocalDateTime(columnIndex: Int): LocalDateTime?

    /**
     * Retrieves the value of the designated column in the current row
     * of this `ResultSet` object as
     * a `Instant` object in the Kotlin programming language.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL `NULL`, the
     * value returned is `null`
     * @throws SQLException if the columnIndex is not valid;
     * if a database access error occurs or this method is
     * called on a closed result set
     */

    fun getInstant(columnIndex: Int): Instant?

}