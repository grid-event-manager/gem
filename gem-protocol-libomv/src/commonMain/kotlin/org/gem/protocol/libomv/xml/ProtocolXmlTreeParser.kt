package org.gem.protocol.libomv.xml

internal object ProtocolXmlTreeParser {
    fun parse(body: ByteArray): ProtocolXmlElement? =
        Parser(body.decodeToString()).parse()

    private class Parser(
        private val source: String,
    ) {
        private var index = 0

        fun parse(): ProtocolXmlElement? {
            skipWhitespace()
            if (isXmlDeclarationStart()) {
                if (!consumeXmlDeclaration()) {
                    return null
                }
                skipWhitespace()
            }
            val root = parseElement() ?: return null
            skipWhitespace()
            return root.takeIf { index == source.length }
        }

        private fun parseElement(): ProtocolXmlElement? {
            if (!consume("<") || startsWith("/") || startsWith("?") || startsWith("!")) {
                return null
            }
            val name = readName().takeIf(String::isNotBlank) ?: return null
            val selfClosing = readOpenTagEnd() ?: return null
            if (selfClosing) {
                return ProtocolXmlElement(name = name, text = "", children = emptyList())
            }

            val text = StringBuilder()
            val children = mutableListOf<ProtocolXmlElement>()
            while (index < source.length) {
                when {
                    startsWith("</") -> {
                        val closingName = readClosingName() ?: return null
                        if (closingName != name) {
                            return null
                        }
                        val elementText = text.toString()
                        if (children.isNotEmpty() && elementText.isNotBlank()) {
                            return null
                        }
                        return ProtocolXmlElement(
                            name = name,
                            text = elementText.takeIf { children.isEmpty() } ?: "",
                            children = children,
                        )
                    }
                    startsWith("<?") -> return null
                    startsWith("<!") -> return null
                    startsWith("<") -> children += parseElement() ?: return null
                    else -> text.append(readText() ?: return null)
                }
            }
            return null
        }

        private fun readOpenTagEnd(): Boolean? {
            var quote: Char? = null
            var selfClosingSlash = false
            while (index < source.length) {
                val char = source[index]
                when {
                    quote != null -> {
                        if (char == quote) {
                            quote = null
                        }
                        index += 1
                    }
                    char == '"' || char == '\'' -> {
                        quote = char
                        index += 1
                    }
                    char == '>' -> {
                        index += 1
                        return selfClosingSlash
                    }
                    char == '/' -> {
                        if (selfClosingSlash) {
                            return null
                        }
                        selfClosingSlash = true
                        index += 1
                    }
                    else -> {
                        if (selfClosingSlash || char == '<') {
                            return null
                        }
                        index += 1
                    }
                }
            }
            return null
        }

        private fun readClosingName(): String? {
            if (!consume("</")) {
                return null
            }
            val name = readName().takeIf(String::isNotBlank) ?: return null
            skipWhitespace()
            return name.takeIf { consume(">") }
        }

        private fun readText(): String? {
            val start = index
            while (index < source.length && source[index] != '<') {
                index += 1
            }
            val raw = source.substring(start, index)
            if (raw.contains("]]>")) {
                return null
            }
            return decodeEntities(raw)
        }

        private fun decodeEntities(raw: String): String? {
            val decoded = StringBuilder()
            var position = 0
            while (position < raw.length) {
                val char = raw[position]
                if (char != '&') {
                    decoded.append(char)
                    position += 1
                    continue
                }
                val end = raw.indexOf(';', startIndex = position + 1)
                if (end == -1) {
                    return null
                }
                decoded.append(
                    when (raw.substring(position + 1, end)) {
                        "amp" -> '&'
                        "lt" -> '<'
                        "gt" -> '>'
                        "quot" -> '"'
                        "apos" -> '\''
                        else -> return null
                    },
                )
                position = end + 1
            }
            return decoded.toString()
        }

        private fun consumeXmlDeclaration(): Boolean {
            if (!isXmlDeclarationStart()) {
                return false
            }
            val end = source.indexOf("?>", startIndex = index + 2)
            if (end == -1) {
                return false
            }
            index = end + 2
            return true
        }

        private fun readName(): String {
            if (index >= source.length || !source[index].isNameStartChar()) {
                return ""
            }
            val start = index
            index += 1
            while (index < source.length && source[index].isNameChar()) {
                index += 1
            }
            return source.substring(start, index)
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) {
                index += 1
            }
        }

        private fun consume(expected: String): Boolean {
            if (!startsWith(expected)) {
                return false
            }
            index += expected.length
            return true
        }

        private fun startsWith(prefix: String): Boolean =
            source.startsWith(prefix, startIndex = index)

        private fun isXmlDeclarationStart(): Boolean =
            startsWith(XML_DECLARATION_START) &&
                source.getOrNull(index + XML_DECLARATION_START.length)?.isWhitespace() == true

        private fun Char.isNameStartChar(): Boolean =
            this in 'A'..'Z' ||
                this in 'a'..'z' ||
                this == '_' ||
                this == ':'

        private fun Char.isNameChar(): Boolean =
            isNameStartChar() ||
                this in '0'..'9' ||
                this == '-' ||
                this == '.'
    }

    private const val XML_DECLARATION_START = "<?xml"
}
