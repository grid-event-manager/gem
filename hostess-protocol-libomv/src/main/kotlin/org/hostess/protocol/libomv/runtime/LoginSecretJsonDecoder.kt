package org.hostess.protocol.libomv.runtime

internal object LoginSecretJsonDecoder {
    fun decode(raw: String): LoginSecret? {
        val fields = StrictStringJsonObjectParser(raw).parse() ?: return null
        if (fields.keys.any { it !in ALLOWED_KEYS }) {
            return null
        }
        val loginUri = fields[LOGIN_URI]?.takeIf(String::isNotBlank) ?: return null
        val firstName = fields[FIRST_NAME]?.takeIf(String::isNotBlank) ?: return null
        val lastName = fields[LAST_NAME]?.takeIf(String::isNotBlank) ?: return null
        val sharedSecret = fields[SHARED_SECRET]?.takeIf(String::isNotBlank) ?: return null
        val startLocation = fields[START_LOCATION]?.takeIf(String::isNotBlank) ?: DEFAULT_START_LOCATION
        return LoginSecret(
            loginUri = loginUri,
            firstName = firstName,
            lastName = lastName,
            sharedSecret = sharedSecret,
            startLocation = startLocation,
        )
    }

    private const val LOGIN_URI = "loginUri"
    private const val FIRST_NAME = "firstName"
    private const val LAST_NAME = "lastName"
    private const val SHARED_SECRET = "sharedSecret"
    private const val START_LOCATION = "startLocation"
    private const val DEFAULT_START_LOCATION = "last"
    private val ALLOWED_KEYS: Set<String> = setOf(LOGIN_URI, FIRST_NAME, LAST_NAME, SHARED_SECRET, START_LOCATION)
}

private class StrictStringJsonObjectParser(
    private val input: String,
) {
    private var index: Int = 0

    fun parse(): Map<String, String>? {
        val fields = linkedMapOf<String, String>()
        skipWhitespace()
        if (!consume('{')) {
            return null
        }
        skipWhitespace()
        if (consume('}')) {
            return finish(fields)
        }
        while (index < input.length) {
            val key = parseString() ?: return null
            if (fields.containsKey(key)) {
                return null
            }
            skipWhitespace()
            if (!consume(':')) {
                return null
            }
            skipWhitespace()
            val value = parseString() ?: return null
            fields[key] = value
            skipWhitespace()
            if (consume('}')) {
                return finish(fields)
            }
            if (!consume(',')) {
                return null
            }
            skipWhitespace()
        }
        return null
    }

    private fun finish(fields: Map<String, String>): Map<String, String>? {
        skipWhitespace()
        return fields.takeIf { index == input.length }
    }

    private fun parseString(): String? {
        if (!consume('"')) {
            return null
        }
        val builder = StringBuilder()
        while (index < input.length) {
            val char = input[index++]
            when {
                char == '"' -> return builder.toString()
                char == '\\' -> builder.append(parseEscape() ?: return null)
                char < ' ' -> return null
                else -> builder.append(char)
            }
        }
        return null
    }

    private fun parseEscape(): Char? {
        if (index >= input.length) {
            return null
        }
        return when (val char = input[index++]) {
            '"', '\\', '/' -> char
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicodeEscape()
            else -> null
        }
    }

    private fun parseUnicodeEscape(): Char? {
        if (index + 4 > input.length) {
            return null
        }
        val hex = input.substring(index, index + 4)
        if (!hex.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            return null
        }
        index += 4
        return hex.toInt(16).toChar()
    }

    private fun skipWhitespace() {
        while (index < input.length && input[index].isWhitespace()) {
            index++
        }
    }

    private fun consume(expected: Char): Boolean {
        if (index >= input.length || input[index] != expected) {
            return false
        }
        index++
        return true
    }
}
