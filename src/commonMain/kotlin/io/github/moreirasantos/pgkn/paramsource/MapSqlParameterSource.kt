package io.github.moreirasantos.pgkn.paramsource

import io.github.moreirasantos.pgkn.sql.SqlParameterValue


/**
 * [SqlParameterSource] implementation that holds a given Map of parameters.
 *
 * The `addValue` methods on this class will make adding several values
 * easier. The methods return a reference to the [MapSqlParameterSource]
 * itself, so you can chain several method calls together within a single statement.
 *
 */
class MapSqlParameterSource : AbstractSqlParameterSource {
    private val values: MutableMap<String, Any?> = LinkedHashMap()

    /**
     * Create an empty MapSqlParameterSource,
     * with values to be added via `addValue`.
     * @see .addValue
     */
    constructor()

    /**
     * Create a new MapSqlParameterSource, with one value
     * comprised of the supplied arguments.
     * @param paramName the name of the parameter
     * @param value the value of the parameter
     * @see .addValue
     */
    constructor(paramName: String, value: Any) {
        addValue(paramName, value)
    }

    /**
     * Create a new MapSqlParameterSource based on a Map.
     * @param values a Map holding existing parameter values (can be `null`)
     */
    constructor(values: Map<String, Any?>?) {
        addValues(values)
    }

    /**
     * Add a parameter to this parameter source.
     * @param paramName the name of the parameter
     * @param value the value of the parameter
     * @return a reference to this parameter source,
     * so it's possible to chain several calls together
     */
    fun addValue(paramName: String, value: Any): MapSqlParameterSource {
        this.values[paramName] = value
        if (value is SqlParameterValue) {
            registerSqlType(paramName, value.sqlType)
        }
        return this
    }

    /**
     * Add a parameter to this parameter source.
     * @param paramName the name of the parameter
     * @param value the value of the parameter
     * @param sqlType the SQL type of the parameter
     * @return a reference to this parameter source,
     * so it's possible to chain several calls together
     */
    fun addValue(paramName: String, value: Any?, sqlType: Int): MapSqlParameterSource {
        this.values[paramName] = value
        registerSqlType(paramName, sqlType)
        return this
    }

    /**
     * Add a parameter to this parameter source.
     * @param paramName the name of the parameter
     * @param value the value of the parameter
     * @param sqlType the SQL type of the parameter
     * @param typeName the type name of the parameter
     * @return a reference to this parameter source,
     * so it's possible to chain several calls together
     */
    fun addValue(paramName: String, value: Any?, sqlType: Int, typeName: String?): MapSqlParameterSource {
        this.values[paramName] = value
        registerSqlType(paramName, sqlType)
        registerTypeName(paramName, typeName!!)
        return this
    }

    /**
     * Add a Map of parameters to this parameter source.
     * @param values a Map holding existing parameter values (can be `null`)
     * @return a reference to this parameter source,
     * so it's possible to chain several calls together
     */
    fun addValues(values: Map<String, Any?>?): MapSqlParameterSource {
        values?.forEach { (key, value) ->
            this.values[key] = value
            if (value is SqlParameterValue) {
                registerSqlType(key, value.sqlType)
            }
        }
        return this
    }

    /**
     * Expose the current parameter values as read-only Map.
     */
    fun getValues(): Map<String, Any?> = this.values

    override fun hasValue(paramName: String) = this.values.containsKey(paramName)

    @Suppress("UseRequire")
    override fun getValue(paramName: String): Any? {
        if (!hasValue(paramName)) {
            throw IllegalArgumentException("No value registered for key '$paramName'")
        }
        return this.values[paramName]
    }

    override val parameterNames: Array<String> get() = this.values.keys.toTypedArray()
}

