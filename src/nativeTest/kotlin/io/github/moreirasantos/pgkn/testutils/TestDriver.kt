package io.github.moreirasantos.pgkn.testutils

import io.github.moreirasantos.pgkn.PostgresDriver

object TestDriver {
    val driver = PostgresDriver(
        host = "localhost",
        port = 5678,
        database = "postgres",
        user = "postgres",
        password = "postgres",
    )
}
