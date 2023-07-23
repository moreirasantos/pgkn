package io.github.moreirasantos.pgkn.sql

import io.github.moreirasantos.pgkn.InvalidDataAccessApiUsageException
import io.github.moreirasantos.pgkn.paramsource.SqlParameterSource


/**
 * Convert parameter declarations from an SqlParameterSource to a corresponding List of SqlParameters.
 * This is necessary in order to reuse existing methods on JdbcTemplate.
 * The SqlParameter for a named parameter is placed in the correct position in the
 * resulting list based on the parsed SQL statement info.
 * @param parsedSql the parsed SQL statement
 * @param paramSource the source for named parameters
 */
internal fun buildSqlParameterList(parsedSql: ParsedSql, paramSource: SqlParameterSource) = parsedSql
    .parameterNames.map {
        SqlParameter(
            it, paramSource.getSqlType(it), paramSource.getTypeName(it)
        )
    }

/**
 * Convert a Map of named parameter values to a corresponding array.
 * @param parsedSql the parsed SQL statement
 * @param paramSource the source for named parameters
 * @param declaredParams the List of declared SqlParameter objects
 * (may be `null`). If specified, the parameter metadata will
 * be built into the value array in the form of SqlParameterValue objects.
 * @return the array of values
 */
@Suppress("NestedBlockDepth")
internal fun buildValueArray(
    parsedSql: ParsedSql,
    paramSource: SqlParameterSource,
    declaredParams: List<SqlParameter>?
): Array<Any?> {
    val paramArray = arrayOfNulls<Any>(parsedSql.totalParameterCount)
    if (parsedSql.namedParameterCount > 0 && parsedSql.unnamedParameterCount > 0) {
        throw InvalidDataAccessApiUsageException(
            "Not allowed to mix named and traditional ? placeholders. You have " +
                    parsedSql.namedParameterCount + " named parameter(s) and " +
                    parsedSql.unnamedParameterCount + " traditional placeholder(s) in statement: " +
                    parsedSql.originalSql
        )
    }
    val paramNames: List<String> = parsedSql.parameterNames
    for (i in paramNames.indices) {
        val paramName = paramNames[i]
        try {
            val param: SqlParameter? = findParameter(declaredParams, paramName, i)
            val paramValue = paramSource.getValue(paramName)
            if (paramValue is SqlParameterValue) {
                paramArray[i] = paramValue
            } else {
                paramArray[i] = (if (param != null) SqlParameterValue(
                    param,
                    (paramValue)!!
                ) else getTypedValue(paramSource, paramName))
            }
        } catch (ex: IllegalArgumentException) {
            throw InvalidDataAccessApiUsageException(
                "No value supplied for the SQL parameter '" + paramName + "': " + ex.message
            )
        }
    }
    return paramArray
}

/**
 * Find a matching parameter in the given list of declared parameters.
 * @param declaredParams the declared SqlParameter objects
 * @param paramName the name of the desired parameter
 * @param paramIndex the index of the desired parameter
 * @return the declared SqlParameter, or `null` if none found
 */
private fun findParameter(
    declaredParams: List<SqlParameter>?,
    paramName: String,
    paramIndex: Int
): SqlParameter? {
    if (declaredParams != null) {
        // First pass: Look for named parameter match.
        for (declaredParam in declaredParams) {
            if (paramName == declaredParam.name) {
                return declaredParam
            }
        }
        // Second pass: Look for parameter index match.
        if (paramIndex < declaredParams.size) {
            val declaredParam = declaredParams[paramIndex]
            // Only accept unnamed parameters for index matches.
            if (declaredParam.name == null) {
                return declaredParam
            }
        }
    }
    return null
}

fun getTypedValue(source: SqlParameterSource, parameterName: String?): Any? {
    val sqlType = source.getSqlType(parameterName!!)
    return if (sqlType != SqlParameterSource.TYPE_UNKNOWN) {
        SqlParameterValue(sqlType, source.getTypeName(parameterName), source.getValue(parameterName)!!)
    } else {
        source.getValue(parameterName)
    }
}

/**
 * Object to represent an SQL parameter definition.
 *
 * Parameters may be anonymous, in which case "name" is `null`.
 * However, all parameters must define an SQL type according to [SqlTypes].
 *
 */
open class SqlParameter {
    /**
     * Return the name of the parameter, or `null` if anonymous.
     */
    // The name of the parameter, if any
    var name: String? = null

    /**
     * Return the SQL type of the parameter.
     */
    // SQL type constant from {@code java.sql.Types}
    val sqlType: Int

    /**
     * Return the type name of the parameter, if any.
     */
    // Used for types that are user-named like: STRUCT, DISTINCT, JAVA_OBJECT, named array types
    var typeName: String? = null

    /**
     * Return the scale of the parameter, if any.
     */
    // The scale to apply in case of a NUMERIC or DECIMAL type, if any
    var scale: Int? = null

    /**
     * Create a new anonymous SqlParameter, supplying the SQL type.
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     */
    constructor(sqlType: Int) {
        this.sqlType = sqlType
    }

    /**
     * Create a new anonymous SqlParameter, supplying the SQL type.
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     * @param typeName the type name of the parameter (optional)
     */
    constructor(sqlType: Int, typeName: String?) {
        this.sqlType = sqlType
        this.typeName = typeName
    }

    /**
     * Create a new anonymous SqlParameter, supplying the SQL type.
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     * @param scale the number of digits after the decimal point
     * (for DECIMAL and NUMERIC types)
     */
    constructor(sqlType: Int, scale: Int) {
        this.sqlType = sqlType
        this.scale = scale
    }

    /**
     * Create a new SqlParameter, supplying name and SQL type.
     * @param name the name of the parameter, as used in input and output maps
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     */
    constructor(name: String?, sqlType: Int) {
        this.name = name
        this.sqlType = sqlType
    }

    /**
     * Create a new SqlParameter, supplying name and SQL type.
     * @param name the name of the parameter, as used in input and output maps
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     * @param typeName the type name of the parameter (optional)
     */
    constructor(name: String?, sqlType: Int, typeName: String?) {
        this.name = name
        this.sqlType = sqlType
        this.typeName = typeName
    }

    /**
     * Create a new SqlParameter, supplying name and SQL type.
     * @param name the name of the parameter, as used in input and output maps
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     * @param scale the number of digits after the decimal point
     * (for DECIMAL and NUMERIC types)
     */
    constructor(name: String?, sqlType: Int, scale: Int) {
        this.name = name
        this.sqlType = sqlType
        this.scale = scale
    }

    /**
     * Copy constructor.
     * @param otherParam the SqlParameter object to copy from
     */
    constructor(otherParam: SqlParameter) {
        name = otherParam.name
        sqlType = otherParam.sqlType
        typeName = otherParam.typeName
        scale = otherParam.scale
    }

    /**
     * Return whether this parameter holds input values that should be set
     * before execution even if they are `null`.
     *
     * This implementation always returns `true`.
     */
    val isInputValueProvided: Boolean get() = true

    /**
     * Return whether this parameter is an implicit return parameter used during the
     * results processing of `CallableStatement.getMoreResults/getUpdateCount`.
     *
     * This implementation always returns `false`.
     */
    val isResultsParameter: Boolean get() = false



    companion object {
        /**
         * Convert a list of JDBC types, as defined in `java.sql.Types`,
         * to a List of SqlParameter objects as used in this package.
         */
        fun sqlTypesToAnonymousParameterList(vararg types: Int) = types.map(::SqlParameter)
    }

    override fun toString(): String {
        return "SqlParameter(name=$name, sqlType=$sqlType, typeName=$typeName, scale=$scale)"
    }
}

/**
 * Object to represent an SQL parameter value, including parameter meta-data
 * such as the SQL type and the scale for numeric values.
 *
 *
 * Designed for use with JdbcTemplate's operations that take an array of
 * argument values: Each such argument value may be a `SqlParameterValue`,
 * indicating the SQL type (and optionally the scale) instead of letting the
 * template guess a default type. Note that this only applies to the operations with
 * a 'plain' argument array, not to the overloaded variants with an explicit type array.
 *
 */
class SqlParameterValue : SqlParameter {
    /**
     * Return the value object that this parameter value holds.
     */
    val value: Any

    /**
     * Create a new SqlParameterValue, supplying the SQL type.
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     * @param value the value object
     */
    constructor(sqlType: Int, value: Any) : super(sqlType) {
        this.value = value
    }

    /**
     * Create a new SqlParameterValue, supplying the SQL type.
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     * @param typeName the type name of the parameter (optional)
     * @param value the value object
     */
    constructor(sqlType: Int, typeName: String?, value: Any) : super(sqlType, typeName) {
        this.value = value
    }

    /**
     * Create a new SqlParameterValue, supplying the SQL type.
     * @param sqlType the SQL type of the parameter according to `java.sql.Types`
     * @param scale the number of digits after the decimal point
     * (for DECIMAL and NUMERIC types)
     * @param value the value object
     */
    constructor(sqlType: Int, scale: Int, value: Any) : super(sqlType, scale) {
        this.value = value
    }

    /**
     * Create a new SqlParameterValue based on the given SqlParameter declaration.
     * @param declaredParam the declared SqlParameter to define a value for
     * @param value the value object
     */
    constructor(declaredParam: SqlParameter, value: Any) : super(declaredParam) {
        this.value = value
    }
}