package org.hostess.protocol.libomv.mapping

import org.hostess.protocol.libomv.xml.ProtocolXmlElement
import org.hostess.protocol.libomv.xml.ProtocolXmlTreeParser

internal object XmlRpcLoginResponseParser {
    fun parseFields(body: ByteArray): Map<String, String>? {
        val methodResponse = ProtocolXmlTreeParser.parse(body)
            ?.takeIf { it.name == "methodResponse" }
            ?: return null
        val struct = methodResponse.normalResponseStruct() ?: methodResponse.faultStruct() ?: return null
        return parseStruct(struct).normalizeFaultFields()
    }

    private fun ProtocolXmlElement.normalResponseStruct(): ProtocolXmlElement? =
        childElement("params")
            ?.childElement("param")
            ?.childElement("value")
            ?.childElement("struct")

    private fun ProtocolXmlElement.faultStruct(): ProtocolXmlElement? =
        childElement("fault")
            ?.childElement("value")
            ?.childElement("struct")

    private fun parseStruct(struct: ProtocolXmlElement): Map<String, String>? {
        val fields = linkedMapOf<String, String>()
        for (member in struct.childElements("member")) {
            val name = member.childElement("name")?.text?.trim()?.takeIf(String::isNotBlank)
                ?: return null
            val valueElement = member.childElement("value") ?: return null
            val scalarValue = parseScalarValue(valueElement) ?: continue
            if (fields.put(name, scalarValue) != null) {
                return null
            }
        }
        return fields
    }

    private fun parseScalarValue(valueElement: ProtocolXmlElement): String? {
        val typed = valueElement.childElements().singleOrNull()
            ?: return valueElement.text.trim().takeIf(String::isNotBlank)
        return when (typed.name) {
            "string", "i4", "int", "double", "dateTime.iso8601", "base64" -> typed.text.trim()
            "boolean" -> when (typed.text.trim()) {
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

    private fun ProtocolXmlElement.childElement(tagName: String): ProtocolXmlElement? =
        childElements(tagName).singleOrNull()

    private fun ProtocolXmlElement.childElements(tagName: String? = null): List<ProtocolXmlElement> =
        children.filter { tagName == null || it.name == tagName }
}
