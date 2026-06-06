package org.hostess.protocol.libomv.llsd

import org.hostess.protocol.libomv.xml.ProtocolXmlElement
import org.hostess.protocol.libomv.xml.ProtocolXmlTreeParser

internal sealed interface LlsdValue {
    data class MapValue(val values: Map<String, LlsdValue>) : LlsdValue
    data class ArrayValue(val values: List<LlsdValue>) : LlsdValue
    data class ScalarValue(val value: String) : LlsdValue
    data class BooleanValue(val value: Boolean) : LlsdValue
    data object Undefined : LlsdValue
}

internal object LlsdXml {
    fun parse(body: ByteArray): LlsdValue? =
        ProtocolXmlTreeParser.parse(body)?.let(::parseElement)

    fun parseMap(body: ByteArray): Map<String, LlsdValue>? =
        (parse(body) as? LlsdValue.MapValue)?.values

    fun parseStringMap(body: ByteArray): Map<String, String>? =
        parseMap(body)?.mapValues { (_, value) -> value.asString() ?: return null }

    private fun parseElement(element: ProtocolXmlElement): LlsdValue? = when (element.name) {
        "llsd" -> element.children.firstNotNullOfOrNull(::parseElement)
        "map" -> parseMapElement(element)
        "array" -> LlsdValue.ArrayValue(element.children.mapNotNull(::parseElement))
        "undef" -> LlsdValue.Undefined
        "boolean" -> element.text.trim().asBoolean()?.let(LlsdValue::BooleanValue)
        "integer", "real", "string", "uuid", "uri" -> LlsdValue.ScalarValue(element.text.trim())
        else -> null
    }

    private fun parseMapElement(element: ProtocolXmlElement): LlsdValue.MapValue {
        val fields = linkedMapOf<String, LlsdValue>()
        var pendingKey: String? = null
        for (child in element.children) {
            if (child.name == "key") {
                pendingKey = child.text.trim()
            } else if (pendingKey != null) {
                val key = pendingKey
                parseElement(child)?.let { fields[key] = it }
                pendingKey = null
            }
        }
        return LlsdValue.MapValue(fields)
    }

    private fun String.asBoolean(): Boolean? = when (lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> null
    }
}

internal fun LlsdValue.asString(): String? = when (this) {
    is LlsdValue.BooleanValue -> value.toString()
    is LlsdValue.ScalarValue -> value
    else -> null
}

internal fun LlsdValue.asLong(): Long? = asString()?.toLongOrNull()

internal fun LlsdValue.asBoolean(): Boolean? = when (this) {
    is LlsdValue.BooleanValue -> value
    is LlsdValue.ScalarValue -> when (value.lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> null
    }
    else -> null
}
