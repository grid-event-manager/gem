package org.gem.core.ports

fun interface RedactionPort {
    fun redact(value: String): String
}
