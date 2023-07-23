package io.github.moreirasantos.pgkn.paramsource

import io.github.moreirasantos.pgkn.paramsource.SqlParameterSource.Companion.TYPE_UNKNOWN
import io.github.moreirasantos.pgkn.sql.SqlTypes


/**
 * Abstract base class for [SqlParameterSource] implementations.
 * Provides registration of SQL types per parameter and a friendly
 * [toString][.toString] representation enumerating all parameters for
 * a `SqlParameterSource` implementing [.getParameterNames].
 * Concrete subclasses must implement [.hasValue] and [.getValue].
 *
 */
abstract class AbstractSqlParameterSource : SqlParameterSource {
    private val sqlTypes: MutableMap<String, Int> = HashMap()
    private val typeNames: MutableMap<String, String> = HashMap()

    /**
     * Register an SQL type for the given parameter.
     * @param paramName the name of the parameter
     * @param sqlType the SQL type of the parameter
     */
    fun registerSqlType(paramName: String, sqlType: Int) {
        sqlTypes[paramName] = sqlType
    }

    /**
     * Register an SQL type for the given parameter.
     * @param paramName the name of the parameter
     * @param typeName the type name of the parameter
     */
    fun registerTypeName(paramName: String, typeName: String) {
        typeNames[paramName] = typeName
    }

    /**
     * Return the SQL type for the given parameter, if registered.
     * @param paramName the name of the parameter
     * @return the SQL type of the parameter,
     * or `TYPE_UNKNOWN` if not registered
     */
    override fun getSqlType(paramName: String) = sqlTypes[paramName] ?: TYPE_UNKNOWN

    /**
     * Return the type name for the given parameter, if registered.
     * @param paramName the name of the parameter
     * @return the type name of the parameter,
     * or `null` if not registered
     */
    fun getTypeName(paramName: String) = typeNames[paramName]

    /**
     * Enumerate the parameter names and values with their corresponding SQL type if available,
     * or just return the simple `SqlParameterSource` implementation class name otherwise.
     * @since 5.2
     * @see .getParameterNames
     */
    @Suppress("NestedBlockDepth")
    override fun toString(): String {
        val parameterNames: Array<String>? = parameterNames
        return if (parameterNames != null) {
            val array = ArrayList<String>(parameterNames.size)
            for (parameterName in parameterNames) {
                val value = getValue(parameterName)
                /*
                if (value is SqlParameterValue) {
                    value = (value as SqlParameterValue?).getValue()
                }
                 */
                var typeName = getTypeName(parameterName)
                if (typeName == null) {
                    val sqlType = getSqlType(parameterName)
                    if (sqlType != TYPE_UNKNOWN) {
                        typeName = sqlTypeNames[sqlType]
                        if (typeName == null) {
                            typeName = sqlType.toString()
                        }
                    }
                }
                val entry = StringBuilder()
                entry.append(parameterName).append('=').append(value)
                if (typeName != null) {
                    entry.append(" (type:").append(typeName).append(')')
                }
                array.add(entry.toString())
            }
            array.joinToString(
                separator = ", ",
                prefix = this::class.simpleName + " {",
                postfix = "}"
            )
        } else {
            this::class.simpleName!!
        }
    }
}

private val sqlTypeNames: Map<Int, String> = SqlTypes.entries.associate { it.value to it.name }

