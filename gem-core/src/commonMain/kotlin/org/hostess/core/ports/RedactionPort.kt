package org.hostess.core.ports

fun interface RedactionPort {
    fun redact(value: String): String
}
