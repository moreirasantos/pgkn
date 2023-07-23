package io.github.moreirasantos.pgkn

import kotlin.test.Test
import kotlin.test.assertEquals

class NamedParametersTest {
    val driver = PostgresDriver(
        host = "localhost",
        port = 5678,
        database = "postgres",
        user = "postgres",
        password = "postgres",
    )

    private val createTable = """
        create table named_params
        (
            id integer not null constraint id primary key,
            name         text,
            email        text,
            int          integer                  default 4
        )
        """.trimIndent()

    @Test
    fun `should select with named params`() {
        driver.execute("drop table if exists named_params")
        driver.execute(createTable)
        assertEquals(0, driver.execute("select * from named_params") {}.size)

        driver.execute("insert into named_params(id, name, email, int) values(1, 'john', 'mail@mail.com', 10)")

        assertEquals(listOf("john"), driver.execute(
            "select name from named_params where name = :one",
            mapOf("one" to "john")
        ) { it.getString(0) })

        assertEquals(listOf("john"), driver.execute(
            "select name from named_params where name = :one OR name = :other",
            mapOf("one" to "john", "other" to "another")
        ) { it.getString(0) })

        assertEquals(emptyList(), driver.execute(
            "select name from named_params where name = :one",
            mapOf("one" to "wrong")
        ) { it.getString(0) })


        driver.execute("drop table named_params")
    }
}
