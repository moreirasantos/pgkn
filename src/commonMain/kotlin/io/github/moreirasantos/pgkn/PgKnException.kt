@file:Suppress("MatchingDeclarationName")

package io.github.moreirasantos.pgkn

open class SQLException(message: String?) : Exception(message) {
    constructor() : this(null)
}

class InvalidDataAccessApiUsageException(message: String) : SQLException(message)
