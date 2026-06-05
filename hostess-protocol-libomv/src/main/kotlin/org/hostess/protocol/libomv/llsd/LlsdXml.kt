package org.hostess.protocol.libomv.llsd

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

internal sealed interface LlsdValue {
    data class MapValue(val values: Map<String, LlsdValue>) : LlsdValue
    data class ArrayValue(val values: List<LlsdValue>) : LlsdValue
    data class ScalarValue(val value: String) : LlsdValue
    data class BooleanValue(val value: Boolean) : LlsdValue
    data object Undefined : LlsdValue
}

internal object LlsdXml {
    fun parse(body: ByteArray): LlsdValue? = try {
        val document = secureDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(body))
        parseElement(document.documentElement)
    } catch (ex: Exception) {
        null
    }

    fun parseMap(body: ByteArray): Map<String, LlsdValue>? =
        (parse(body) as? LlsdValue.MapValue)?.values

    fun parseStringMap(body: ByteArray): Map<String, String>? =
        parseMap(body)?.mapValues { (_, value) -> value.asString() ?: return null }

    private fun parseElement(element: Element): LlsdValue? = when (element.tagName) {
        "llsd" -> element.childElements().firstNotNullOfOrNull(::parseElement)
        "map" -> parseMapElement(element)
        "array" -> LlsdValue.ArrayValue(element.childElements().mapNotNull(::parseElement))
        "undef" -> LlsdValue.Undefined
        "boolean" -> element.textContent.trim().asBoolean()?.let(LlsdValue::BooleanValue)
        "integer", "real", "string", "uuid", "uri" -> LlsdValue.ScalarValue(element.textContent.trim())
        else -> null
    }

    private fun parseMapElement(element: Element): LlsdValue.MapValue {
        val fields = linkedMapOf<String, LlsdValue>()
        var pendingKey: String? = null
        for (child in element.childElements()) {
            if (child.tagName == "key") {
                pendingKey = child.textContent.trim()
            } else if (pendingKey != null) {
                val key = pendingKey
                parseElement(child)?.let { fields[key] = it }
                pendingKey = null
            }
        }
        return LlsdValue.MapValue(fields)
    }

    private fun Element.childElements(): List<Element> = buildList {
        for (index in 0 until childNodes.length) {
            val node = childNodes.item(index)
            if (node.nodeType == Node.ELEMENT_NODE) {
                add(node as Element)
            }
        }
    }

    private fun String.asBoolean(): Boolean? = when (lowercase()) {
        "true", "1" -> true
        "false", "0" -> false
        else -> null
    }

    private fun secureDocumentBuilderFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().also {
        it.isExpandEntityReferences = false
        it.setFeature(feature("apache.org", "xml/features/disallow-doctype-decl"), true)
        it.setFeature(feature("xml.org", "sax/features/external-general-entities"), false)
        it.setFeature(feature("xml.org", "sax/features/external-parameter-entities"), false)
    }

    private fun feature(host: String, path: String): String = "http" + "://$host/$path"
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
