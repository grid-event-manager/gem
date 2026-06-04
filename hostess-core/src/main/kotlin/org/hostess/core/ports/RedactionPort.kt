package org.hostess.core.ports

interface RedactionPort {
    fun redact(value: String): String
}
