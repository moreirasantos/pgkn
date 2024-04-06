package io.github.moreirasantos.pgkn

import io.r2dbc.spi.R2dbcBadGrammarException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PostgresDriverTest {

    @Test
    fun `should fail with non existent table`() {
        runBlocking {
            val driver = PostgresDriver(
                host = "localhost",
                port = 5678,
                database = "postgres",
                user = "postgres",
                password = "postgres",
            )
            assertFailsWith<R2dbcBadGrammarException> {
                driver.execute("select * from nonexistent") {}
            }
        }
    }


    @Test
    fun `should create and delete table`() {
        runBlocking {
            val driver = PostgresDriver(
                host = "localhost",
                port = 5678,
                database = "postgres",
                user = "postgres",
                password = "postgres",
            )
            driver.execute("drop table if exists to_create")
            driver.execute("create table to_create(id serial primary key)")

            assertEquals(0, driver.execute("select * from to_create") {}.size)

            driver.execute("drop table to_create")
        }
    }
}
