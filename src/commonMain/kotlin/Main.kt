import me.miguel.pgkn.PostgresDriver


fun main() {
    val driver = PostgresDriver(
        host = "host.docker.internal",
        port = 5432,
        user = "postgres",
        database = "postgres",
        password = "postgres",
    )

    val list = driver.execute("SELECT * FROM users") {
        mapOf(
            "id" to it.getLong(0),
            "name" to it.getString(1),
            "email" to it.getString(2),
            "bool" to it.getBoolean(3),
            "short" to it.getShort(4),
            "int" to it.getInt(5),
            "float" to it.getFloat(6),
            "double" to it.getDouble(7),
            "bytea" to it.getBytes(8),
            "date" to it.getDate(9),
            "time" to it.getTime(10),
            "timestamp" to it.getLocalDateTime(11),
            "timestamp with time zone" to it.getInstant(12),
        )
    }

    println(list)
}
