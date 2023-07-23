@file:Suppress("MatchingDeclarationName")

package io.github.moreirasantos.pgkn

open class SQLException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class InvalidDataAccessApiUsageException(message: String, cause: Throwable? = null) : SQLException(message, cause)
