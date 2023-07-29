package io.github.moreirasantos.pgkn

import kotlinx.datetime.*
import io.github.moreirasantos.pgkn.resultset.ResultSet
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostgresDriverTest {
    val driver = PostgresDriver(
        host = "localhost",
        port = 5678,
        database = "postgres",
        user = "postgres",
        password = "postgres",
    )

    @Test
    fun `should fail with non existent table`() {
        assertFailsWith<IllegalStateException> {
            driver.execute("select * from nonexistent") {}
        }
    }

    @Test
    fun `should create and delete table`() {
        driver.execute("drop table if exists to_create")
        driver.execute("create table to_create(id serial primary key)")

        assertEquals(0, driver.execute("select * from to_create") {}.size)

        driver.execute("drop table to_create")
    }

    @Test
    fun `should insert select update and delete row`() {
        val column = "test"
        val insertValue = "insert-value"
        val updateValue = "update-value"

        driver.execute("drop table if exists to_crud")
        driver.execute("create table to_crud(id serial primary key, $column varchar (50))")

        assertEquals(0, driver.execute("select * from to_crud") {}.size)

        driver.execute("insert into to_crud($column) values('$insertValue')")

        assertEquals(
            insertValue,
            driver.execute("select $column from to_crud") { it.getString(0) }.first()
        )

        assertEquals(1, driver.execute("update to_crud set $column = '$updateValue'"))

        assertEquals(
            updateValue,
            driver.execute("select $column from to_crud") { it.getString(0) }.first()
        )

        assertEquals(1, driver.execute("delete from to_crud where $column = '$updateValue'"))
        assertEquals(0, driver.execute("select * from to_crud") {}.size)
        driver.execute("drop table to_crud")
    }

    private class Holder<T : Any>(
        val column: String,
        val value: T,
        val insertValue: Any = value,
        val extractor: (ResultSet) -> T?
    )

    private val createTable = """
        create table all_types
        (
            id integer not null constraint id primary key,
            name         text,
            email        text,
            bool         boolean                  default true,
            short        smallint                 default 3,
            int          integer                  default 4,
            float        double precision,
            double       double precision         default 5.678912,
            bytea    bytea                    default decode('DEADBEEF'::text, 'hex'::text),
            date         date                     default now(),
            time         time                     default now(),
            timestamp    timestamp                default now(),
            timestamp_tz timestamp with time zone default now()
        )
        """.trimIndent()


    private fun Int.toTimeDigit() = toString().padStart(2, '0')

    @Test
    fun `should select all column types`() {
        val now = Clock.System.now().let { Instant.fromEpochSeconds(it.epochSeconds, 0) }
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())

        val byteArrayHolder = with("\\xdeadbeef".encodeToByteArray()) {
            Holder(
                "bytea",
                this, "'${this.decodeToString()}'"
            ) { it.getBytes(0) }
        }

        val list = listOf(
            Holder("id", 123L) { it.getLong(0) },
            Holder("name", "name", "'name'") { it.getString(0) },
            Holder("email", "mail@mail.com", "'mail@mail.com'") { it.getString(0) },
            Holder("bool", true) { it.getBoolean(0) },
            Holder("short", Short.MIN_VALUE) { it.getShort(0) },
            Holder("int", 234) { it.getInt(0) },
            Holder("float", 1.23f) { it.getFloat(0) },
            Holder("double", 2.34) { it.getDouble(0) },
            byteArrayHolder,
            with(Clock.System.todayIn(TimeZone.currentSystemDefault())) {
                Holder("date", this, "'$this'") { it.getDate(0) }
            },
            with(
                LocalTime(localDateTime.time.hour, localDateTime.time.minute, localDateTime.time.second, 0)
            ) {
                Holder("time", this, this.let {
                    "'${it.hour.toTimeDigit()}:${it.minute.toTimeDigit()}:${it.second.toTimeDigit()}'"
                }) { it.getTime(0) }
            },
            Holder("timestamp", localDateTime, "'$localDateTime'") { it.getLocalDateTime(0) },
            Holder("timestamp_tz", now, "'$now'") { it.getInstant(0) },
        )

        driver.execute("drop table if exists all_types")
        driver.execute(createTable)
        assertEquals(0, driver.execute("select * from all_types") {}.size)

        driver.execute(
            "insert into all_types(${
                list.joinToString(separator = ", ") { it.column }
            }) values(${
                list.joinToString(separator = ", ") { it.insertValue.toString() }
            })"
        )

        list
            .asSequence()
            .filter { it.value !is ByteArray }
            .forEach {
                assertEquals(
                    it.value,
                    driver.execute("select ${it.column} from all_types") { rs -> it.extractor(rs) }.first()
                )
            }

        with(byteArrayHolder) {
            val select = driver.execute("select ${this.column} from all_types") { rs -> this.extractor(rs) }
                .first()!!
            this.value.forEach { byte -> assertContains(select, byte) }
        }
        driver.execute("drop table all_types")
    }
}
