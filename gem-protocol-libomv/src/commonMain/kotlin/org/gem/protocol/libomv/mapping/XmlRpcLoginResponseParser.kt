package org.gem.protocol.libomv.mapping

import org.gem.protocol.libomv.xml.ProtocolXmlElement
import org.gem.protocol.libomv.xml.ProtocolXmlTreeParser

internal object XmlRpcLoginResponseParser {
    fun parseFields(body: ByteArray): Map<String, String>? {
        val methodResponse = ProtocolXmlTreeParser.parse(body)
            ?.takeIf { it.name == "methodResponse" }
            ?: return null
        val struct = methodResponse.normalResponseStruct() ?: methodResponse.faultStruct() ?: return null
        return parseStruct(struct).normalizeFaultFields()
    }

    fun parseInventoryRoots(body: String): LoginInventoryRoots {
        val methodResponse = ProtocolXmlTreeParser.parse(body.encodeToByteArray())
            ?.takeIf { it.name == "methodResponse" }
            ?: return LoginInventoryRoots.empty()
        val struct = methodResponse.normalResponseStruct() ?: return LoginInventoryRoots.empty()
        val fields = parseXmlRpcStruct(struct) ?: return LoginInventoryRoots.empty()
        val agentId = fields[LoginKeys.AGENT_ID]?.asScalar()?.takeIf(String::isNotBlank)
        val libraryOwnerId = mappedId(fields[LoginKeys.INVENTORY_LIB_OWNER], LoginKeys.AGENT_ID)
        return LoginInventoryRoots(
            inventoryRootId = mappedId(fields[LoginKeys.INVENTORY_ROOT], LoginKeys.FOLDER_ID),
            inventorySkeleton = skeletonFolders(
                value = fields[LoginKeys.INVENTORY_SKELETON],
                defaultOwnerId = agentId,
            ),
            libraryRootId = mappedId(fields[LoginKeys.INVENTORY_LIB_ROOT], LoginKeys.FOLDER_ID),
            libraryOwnerId = libraryOwnerId,
            librarySkeleton = skeletonFolders(
                value = fields[LoginKeys.INVENTORY_SKEL_LIB],
                defaultOwnerId = libraryOwnerId,
            ),
        )
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

    private fun parseXmlRpcStruct(struct: ProtocolXmlElement): Map<String, XmlRpcValue>? {
        val fields = linkedMapOf<String, XmlRpcValue>()
        for (member in struct.childElements("member")) {
            val name = member.childElement("name")?.text?.trim()?.takeIf(String::isNotBlank)
                ?: return null
            val valueElement = member.childElement("value") ?: return null
            val value = parseXmlRpcValue(valueElement) ?: return null
            if (fields.put(name, value) != null) {
                return null
            }
        }
        return fields
    }

    private fun parseXmlRpcValue(valueElement: ProtocolXmlElement): XmlRpcValue? {
        val typed = valueElement.childElements().singleOrNull()
            ?: return valueElement.text.trim()
                .takeIf(String::isNotBlank)
                ?.let(XmlRpcValue::Scalar)
        return when (typed.name) {
            "string", "i4", "int", "double", "dateTime.iso8601", "base64" ->
                XmlRpcValue.Scalar(typed.text.trim())
            "boolean" -> when (typed.text.trim()) {
                "1", "true" -> XmlRpcValue.Scalar("true")
                "0", "false" -> XmlRpcValue.Scalar("false")
                else -> null
            }
            "array" -> parseXmlRpcArray(typed)
            "struct" -> parseXmlRpcStruct(typed)?.let(XmlRpcValue::Struct)
            else -> null
        }
    }

    private fun parseXmlRpcArray(arrayElement: ProtocolXmlElement): XmlRpcValue.Array? {
        val data = arrayElement.childElement("data") ?: return null
        return XmlRpcValue.Array(
            data.childElements("value").mapNotNull(::parseXmlRpcValue),
        )
    }

    private fun mappedId(value: XmlRpcValue?, fieldName: String): String? {
        val array = value as? XmlRpcValue.Array ?: return null
        val root = array.values.singleOrNull() as? XmlRpcValue.Struct ?: return null
        return root.fields[fieldName]?.asScalar()?.takeIf(String::isNotBlank)
    }

    private fun skeletonFolders(
        value: XmlRpcValue?,
        defaultOwnerId: String?,
    ): List<LoginInventoryFolder> {
        val array = value as? XmlRpcValue.Array ?: return emptyList()
        return array.values.mapNotNull { entry ->
            val fields = (entry as? XmlRpcValue.Struct)?.fields ?: return@mapNotNull null
            val folderId = fields[LoginKeys.FOLDER_ID]?.asScalar()?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            LoginInventoryFolder(
                folderId = folderId,
                parentId = fields[LoginKeys.PARENT_ID]?.asScalar()?.takeIf(String::isNotBlank),
                ownerId = defaultOwnerId?.takeIf(String::isNotBlank),
                name = fields[LoginKeys.NAME]?.asScalar()?.takeIf(String::isNotBlank),
                typeDefault = fields[LoginKeys.TYPE_DEFAULT]?.asInt(),
                version = fields[LoginKeys.VERSION_FIELD]?.asInt(),
            )
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

    private fun XmlRpcValue.asScalar(): String? = (this as? XmlRpcValue.Scalar)?.value

    private fun XmlRpcValue.asInt(): Int? = asScalar()
        ?.toLongOrNull()
        ?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }
        ?.toInt()

    private sealed interface XmlRpcValue {
        data class Scalar(val value: String) : XmlRpcValue
        data class Array(val values: List<XmlRpcValue>) : XmlRpcValue
        data class Struct(val fields: Map<String, XmlRpcValue>) : XmlRpcValue
    }
}
