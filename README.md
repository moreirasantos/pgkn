
[![Kotlin Experimental](https://kotl.in/badges/experimental.svg)](https://kotlinlang.org/docs/components-stability.html)
[![CI](https://github.com/miguel-moreira/pgkn/actions/workflows/blank.yml/badge.svg?branch=main)](https://github.com/miguel-moreira/pgkn/actions/workflows/blank.yml)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.moreirasantos/pgkn)](https://central.sonatype.com/artifact/io.github.moreirasantos/pgkn/)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)


# pgkn
PostgreSQL Kotlin/Native Driver

## Usage
```
// Show full structure of a kotlin native project
implementation("io.github.moreirasantos:pgkn:1.0.0")
```
```
fun main() {
    val driver = PostgresDriver(
        host = "host.docker.internal",
        port = 5432,
        user = "postgres",
        database = "postgres",
        password = "postgres",
    )

    driver.execute("CREATE TABLE my_table(id serial primary key)")
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
}
```
