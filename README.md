[![Kotlin Experimental](https://kotl.in/badges/experimental.svg)](https://kotlinlang.org/docs/components-stability.html)
[![CI](https://github.com/miguel-moreira/pgkn/actions/workflows/blank.yml/badge.svg?branch=main)](https://github.com/miguel-moreira/pgkn/actions/workflows/blank.yml)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.moreirasantos/pgkn)](https://central.sonatype.com/artifact/io.github.moreirasantos/pgkn/)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)

# pgkn

PostgreSQL Kotlin/Native Driver

## Usage

```
implementation("io.github.moreirasantos:pgkn:1.0.0")
```

```kotlin
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

## Features

### Connection Pool

PGKN has a connection pool, its size being configurable in `PostgresDriver()` - 20 by default.  
It will refresh connection units if the query fails fatally, but it still needs more fine-grained status checks.


You can also use a single connection with `PostgresDriverUnit`, which currently is not `suspend`
but probably will be in the future.

### Named Parameters

```kotlin
driver.execute(
    "select name from my_table where name = :one OR email = :other",
    mapOf("one" to "your_name", "other" to "your@email.com")
) { it.getString(0) }
```

Named Parameters provides an alternative to the traditional syntax using `?` to specify parameters.
Under the hood, it substitutes the named parameters to a query placeholder.

In JDBC, the placeholder would be `?` but with libpq, we will pass `$1`, `$2`, etc as stated here:
[31.3.1. Main Functions - PQexecParams](https://www.postgresql.org/docs/9.5/libpq-exec.html)

This feature implementation tries to follow Spring's `NamedParameterJdbcTemplate` as close as possible.
[NamedParameterJdbcTemplate](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/NamedParameterJdbcTemplate.html)
