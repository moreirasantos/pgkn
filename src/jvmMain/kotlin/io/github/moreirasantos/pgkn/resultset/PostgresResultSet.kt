package io.github.moreirasantos.pgkn.resultset

import io.github.moreirasantos.pgkn.exception.IllegalResultSetAccessException
import io.r2dbc.spi.Readable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@Suppress("TooManyFunctions")
internal class PostgresResultSet(private val row: Readable) : ResultSet {
    override fun next(): Boolean {
        // TODO: Remove next() from ResultSet or create Row Interface
        throw IllegalResultSetAccessException()
    }

    override fun getString(columnIndex: Int) = row.get(columnIndex) as String?

    override fun getBoolean(columnIndex: Int) = row.get(columnIndex) as Boolean?

    override fun getShort(columnIndex: Int) = row.get(columnIndex) as Short?

    override fun getInt(columnIndex: Int) = row.get(columnIndex) as Int?

    override fun getLong(columnIndex: Int) = row.get(columnIndex) as Long?

    override fun getFloat(columnIndex: Int) = row.get(columnIndex) as Float?

    override fun getDouble(columnIndex: Int) = row.get(columnIndex) as Double?

    override fun getBytes(columnIndex: Int) = row.get(columnIndex) as ByteArray?

    // TODO: Date parsing
    override fun getDate(columnIndex: Int) = row.get(columnIndex) as LocalDate?

    override fun getTime(columnIndex: Int) = row.get(columnIndex) as LocalTime?

    override fun getLocalDateTime(columnIndex: Int) = row.get(columnIndex) as LocalDateTime?

    override fun getInstant(columnIndex: Int) = row.get(columnIndex) as Instant?

}
