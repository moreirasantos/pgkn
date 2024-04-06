package io.github.moreirasantos.pgkn

import kotlin.test.Test
import kotlin.test.assertEquals

class DriverUnitTest {
    @Test
    fun `single driver should work`() {
        val driver = PostgresDriverUnit(
            host = "localhost",
            port = 5678,
            database = "postgres",
            user = "postgres",
            password = "postgres"
        )
        assertEquals("echo", driver
            .execute("select 'echo'") { it.getString(0) }
            .first())
    }
}
