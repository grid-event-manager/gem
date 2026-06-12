package org.gem.protocol.libomv.runtime

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

internal object LoginPackageCaptureNormalizer {
    fun normalize(xml: String): NormalizedLoginPackage {
        val document = documentBuilderFactory()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
        val methodCall = document.documentElement.requireTag("methodCall")
        val methodName = methodCall.childElement("methodName").textContent
        val struct = methodCall
            .childElement("params")
            .childElement("param")
            .childElement("value")
            .childElement("struct")
        val fields = linkedMapOf<String, NormalizedLoginField>()
        struct.childElements("member").forEach { member ->
            val name = member.childElement("name").textContent
            val value = parseValue(member.childElement("value"))
            require(fields.put(name, value) == null) { "duplicate XML-RPC field: $name" }
        }
        return NormalizedLoginPackage(methodName = methodName, fields = fields)
    }

    private fun documentBuilderFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }

    private fun parseValue(valueElement: Element): NormalizedLoginField {
        val typedValue = valueElement.childElements().singleOrNull()
            ?: error("XML-RPC value must contain exactly one typed value")
        return when (typedValue.tagName) {
            "string" -> NormalizedLoginString("string", typedValue.textContent)
            "i4" -> NormalizedLoginInteger("i4", typedValue.textContent.toInt())
            "array" -> parseArray(typedValue)
            else -> error("unsupported XML-RPC type: ${typedValue.tagName}")
        }
    }

    private fun parseArray(arrayElement: Element): NormalizedLoginStringArray {
        val values = arrayElement
            .childElement("data")
            .childElements("value")
            .map { valueElement ->
                val stringValue = valueElement.childElements().singleOrNull()
                    ?: error("array item must contain exactly one typed value")
                require(stringValue.tagName == "string") { "array item must be an XML-RPC string" }
                stringValue.textContent
            }
        return NormalizedLoginStringArray("array<string>", values)
    }

    private fun Element.requireTag(expected: String): Element {
        require(tagName == expected) { "expected <$expected> but found <$tagName>" }
        return this
    }

    private fun Element.childElement(tagName: String): Element =
        childElements(tagName).singleOrNull()
            ?: error("expected exactly one <$tagName> child under <$this.tagName>")

    private fun Element.childElements(tagName: String? = null): List<Element> =
        (0 until childNodes.length)
            .mapNotNull { childNodes.item(it) as? Element }
            .filter { tagName == null || it.tagName == tagName }
}

internal data class NormalizedLoginPackage(
    val methodName: String,
    val fields: Map<String, NormalizedLoginField>,
)

internal sealed interface NormalizedLoginField {
    val xmlRpcType: String
}

internal data class NormalizedLoginString(
    override val xmlRpcType: String,
    val value: String,
) : NormalizedLoginField

internal data class NormalizedLoginInteger(
    override val xmlRpcType: String,
    val value: Int,
) : NormalizedLoginField

internal data class NormalizedLoginStringArray(
    override val xmlRpcType: String,
    val value: List<String>,
) : NormalizedLoginField
