package me.miguel.pgkn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostgresDriverTest {

    private val driver = PostgresDriver(
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

        assertEquals( 1, driver.execute("update to_crud set $column = '$updateValue'"))

        assertEquals(
            updateValue,
            driver.execute("select $column from to_crud") { it.getString(0) }.first()
        )

        assertEquals( 1, driver.execute("delete from to_crud where $column = '$updateValue'"))
        assertEquals( 0, driver.execute("select * from to_crud") {}.size)
        driver.execute("drop table to_crud")
    }
}
