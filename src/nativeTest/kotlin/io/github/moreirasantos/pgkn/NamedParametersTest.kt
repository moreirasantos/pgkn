package io.github.moreirasantos.pgkn

import io.github.moreirasantos.pgkn.testutils.TestDriver.driver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class NamedParametersTest {
    private fun createTable(name: String) = """
        create table $name
        (
            id integer not null constraint id primary key,
            name         text,
            email        text,
            int          integer                  default 4
        )
        """.trimIndent()

    @Test
    fun `should select with named params`() {
        runBlocking {
            val t = "named_params"
            driver.execute("drop table if exists $t")
            driver.execute(createTable(t))
            assertEquals(0, driver.execute("select * from $t") {}.size)

            driver.execute("insert into $t(id, name, email, int) values(1, 'john', 'mail@mail.com', 10)")

            assertEquals(listOf("john"), driver.execute(
                "select name from $t where name = :one",
                mapOf("one" to "john")
            ) { it.getString(0) })

            assertEquals(listOf("john"), driver.execute(
                "select name from $t where name = :one OR name = :other",
                mapOf("one" to "john", "other" to "another")
            ) { it.getString(0) })

            assertEquals(emptyList(), driver.execute(
                "select name from $t where name = :one",
                mapOf("one" to "wrong")
            ) { it.getString(0) })

            driver.execute("drop table $t")
        }
    }

    @Test
    fun `should update with named params`() {
        runBlocking {
            val t = "named_params_update"
            driver.execute("drop table if exists $t")
            driver.execute(createTable(t))
            assertEquals(0, driver.execute("select * from $t") {}.size)

            driver.execute("insert into $t(id, name, email, int) values(1, 'john', 'mail@mail.com', 10)")

            assertEquals(
                1, driver.execute(
                    "update $t set int = :number where name = :one",
                    mapOf("one" to "john", "number" to 15)
                )
            )

            assertEquals(listOf("john"), driver.execute(
                "select name from $t where int = :number",
                mapOf("number" to 15)
            ) { it.getString(0) })

            driver.execute("drop table $t")
        }
    }
}
