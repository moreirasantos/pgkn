package me.miguel.pgkn

import kotlin.test.Test
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
            driver.executeQuery("Select * from nonexistent") {}
        }
    }
}