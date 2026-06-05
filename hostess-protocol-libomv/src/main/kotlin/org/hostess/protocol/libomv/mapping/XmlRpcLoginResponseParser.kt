package org.hostess.protocol.libomv.mapping

import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

internal object XmlRpcLoginResponseParser {
    fun parseFields(body: ByteArray): Map<String, String>? = try {
        val document = secureDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(body))
        val methodResponse = document.documentElement.takeIf { it.tagName == "methodResponse" } ?: return null
        val struct = methodResponse.normalResponseStruct() ?: methodResponse.faultStruct() ?: return null
        parseStruct(struct).normalizeFaultFields()
    } catch (ex: Exception) {
        null
    }

    private fun Element.normalResponseStruct(): Element? =
        childElement("params")
            ?.childElement("param")
            ?.childElement("value")
            ?.childElement("struct")

    private fun Element.faultStruct(): Element? =
        childElement("fault")
            ?.childElement("value")
            ?.childElement("struct")

    private fun parseStruct(struct: Element): Map<String, String>? {
        val fields = linkedMapOf<String, String>()
        for (member in struct.childElements("member")) {
            val name = member.childElement("name")?.textContent?.trim()?.takeIf(String::isNotBlank)
                ?: return null
            val valueElement = member.childElement("value") ?: return null
            val scalarValue = parseScalarValue(valueElement) ?: continue
            if (fields.put(name, scalarValue) != null) {
                return null
            }
        }
        return fields
    }

    private fun parseScalarValue(valueElement: Element): String? {
        val typed = valueElement.childElements().singleOrNull()
            ?: return valueElement.textContent.trim().takeIf(String::isNotBlank)
        return when (typed.tagName) {
            "string", "i4", "int", "double", "dateTime.iso8601", "base64" -> typed.textContent.trim()
            "boolean" -> when (typed.textContent.trim()) {
                "1", "true" -> "true"
                "0", "false" -> "false"
                else -> null
            }
            else -> null
        }
    }

    private fun Map<String, String>?.normalizeFaultFields(): Map<String, String>? {
        val fields = this ?: return null
        val faultString = fields["faultString"]?.takeIf(String::isNotBlank) ?: return fields
        return fields + (LoginKeys.MESSAGE to faultString) + (LoginKeys.LOGIN to "false")
    }

    private fun Element.childElement(tagName: String): Element? =
        childElements(tagName).singleOrNull()

    private fun Element.childElements(tagName: String? = null): List<Element> = buildList {
        for (index in 0 until childNodes.length) {
            val node = childNodes.item(index)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                if (tagName == null || element.tagName == tagName) {
                    add(element)
                }
            }
        }
    }

    private fun secureDocumentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().also {
            it.isNamespaceAware = false
            it.isXIncludeAware = false
            it.isExpandEntityReferences = false
            it.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            it.setFeature(feature("apache.org", "xml/features/disallow-doctype-decl"), true)
            it.setFeature(feature("xml.org", "sax/features/external-general-entities"), false)
            it.setFeature(feature("xml.org", "sax/features/external-parameter-entities"), false)
            it.setFeature(feature("apache.org", "xml/features/nonvalidating/load-external-dtd"), false)
        }

    private fun feature(host: String, path: String): String = "http" + "://$host/$path"
}
