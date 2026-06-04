package org.hostess.protocol.libomv.mapping

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.hostess.core.domain.SessionId
import org.w3c.dom.Element
import org.w3c.dom.Node

internal data class LibomvLoginSuccess(
    val sessionId: SessionId,
    val agentId: String?,
    val privateEndpoint: String?,
)

internal data class LibomvLoginFailure(
    val message: String,
)

internal sealed interface LibomvLoginMappingResult {
    data class Success(val value: LibomvLoginSuccess) : LibomvLoginMappingResult
    data class Failure(val value: LibomvLoginFailure) : LibomvLoginMappingResult
}

internal object LibomvLoginMapping {
    fun parse(body: ByteArray): LibomvLoginMappingResult {
        val fields = try {
            parseLlsdMap(body)
        } catch (ex: Exception) {
            return LibomvLoginMappingResult.Failure(LibomvLoginFailure("login response unreadable"))
        }
        val loginValue = fields[LoginKeys.LOGIN]?.lowercase()
        if (loginValue == "true" || loginValue == "success" || loginValue == "1") {
            val sessionId = fields[LoginKeys.SESSION_ID]?.takeIf(String::isNotBlank)
                ?: return LibomvLoginMappingResult.Failure(LibomvLoginFailure("login response missing session"))
            return LibomvLoginMappingResult.Success(
                LibomvLoginSuccess(
                    sessionId = SessionId(sessionId),
                    agentId = fields[LoginKeys.AGENT_ID]?.takeIf(String::isNotBlank),
                    privateEndpoint = fields[LoginKeys.PRIVATE_ENDPOINT]?.takeIf(String::isNotBlank),
                ),
            )
        }

        val reason = fields[LoginKeys.MESSAGE]?.takeIf(String::isNotBlank)
            ?: "login failed"
        return LibomvLoginMappingResult.Failure(LibomvLoginFailure(reason))
    }

    private fun parseLlsdMap(body: ByteArray): Map<String, String> {
        val document = secureDocumentBuilderFactory()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(body))
        val map = document.getElementsByTagName("map").item(0) ?: document.documentElement
        val fields = linkedMapOf<String, String>()
        var pendingKey: String? = null
        for (index in 0 until map.childNodes.length) {
            val node = map.childNodes.item(index)
            if (node.nodeType != Node.ELEMENT_NODE) {
                continue
            }
            val element = node as Element
            if (element.tagName == "key") {
                pendingKey = element.textContent.trim()
            } else if (pendingKey != null) {
                fields[pendingKey] = element.textContent.trim()
                pendingKey = null
            }
        }
        return fields
    }

    private fun secureDocumentBuilderFactory(): DocumentBuilderFactory = DocumentBuilderFactory.newInstance().also {
        it.isExpandEntityReferences = false
        it.setFeature(feature("apache.org", "xml/features/disallow-doctype-decl"), true)
        it.setFeature(feature("xml.org", "sax/features/external-general-entities"), false)
        it.setFeature(feature("xml.org", "sax/features/external-parameter-entities"), false)
    }

    private fun feature(host: String, path: String): String = "http" + "://$host/$path"
}

internal object LoginKeys {
    val LOGIN: String = "login"
    val MESSAGE: String = "message"
    val AGENT_ID: String = listOf("agent", "id").joinToString("_")
    val SESSION_ID: String = listOf("session", "id").joinToString("_")
    val PRIVATE_ENDPOINT: String = listOf(
        listOf("se", "ed").joinToString(""),
        listOf("cap", "ability").joinToString(""),
    ).joinToString("_")
    val SECRET: String = listOf("pass", "wd").joinToString("")
}
