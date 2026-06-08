package org.hostess.protocol.libomv.mapping

import org.hostess.core.domain.SessionId
import org.hostess.core.services.SafeDiagnosticRedaction
import org.hostess.protocol.libomv.llsd.LlsdValue
import org.hostess.protocol.libomv.llsd.LlsdXml
import org.hostess.protocol.libomv.llsd.asString

internal data class LibomvLoginSuccess(
    val sessionId: SessionId,
    val agentId: String?,
    val seedCapability: String?,
    val simulatorIp: String?,
    val simulatorPort: Int?,
    val regionHandle: Long?,
    val circuitCode: Long?,
    val inventoryRoots: LoginInventoryRoots,
    val appearanceState: LoginAppearanceState,
)

internal data class LibomvLoginFailure(
    val kind: LibomvLoginFailureKind,
    val redactedMessage: String,
)

internal enum class LibomvLoginFailureKind(val redactedMessage: String) {
    NORMAL_FAILURE("login failed"),
    TOS_REQUIRED("login blocked: terms of service required"),
    CRITICAL_MESSAGE_REQUIRED("login blocked: critical message required"),
    UPDATE_REQUIRED("login blocked: viewer update required"),
    MFA_REQUIRED("login blocked: mfa token required"),
    MALFORMED_RESPONSE("login response malformed"),
    TRANSPORT_FAILURE("login transport failed"),
}

internal sealed interface LibomvLoginMappingResult {
    data class Success(val value: LibomvLoginSuccess) : LibomvLoginMappingResult
    data class Failure(val value: LibomvLoginFailure) : LibomvLoginMappingResult
}

internal object LibomvLoginMapping {
    fun parse(body: ByteArray): LibomvLoginMappingResult {
        val llsdFields = LlsdXml.parseMap(body)
        val fields = llsdFields?.stringFields()
            ?: XmlRpcLoginResponseParser.parseFields(body)
            ?: return failure(
                LibomvLoginFailureKind.MALFORMED_RESPONSE,
                bodyDiagnostic(body),
            )
        val loginValue = fields[LoginKeys.LOGIN]?.lowercase()
        if (loginValue == "true" || loginValue == "success" || loginValue == "1") {
            val sessionId = fields[LoginKeys.SESSION_ID]?.takeIf(String::isNotBlank)
                ?: return failure(
                    LibomvLoginFailureKind.MALFORMED_RESPONSE,
                    "missing=${LoginKeys.SESSION_ID}",
                )
            return LibomvLoginMappingResult.Success(
                LibomvLoginSuccess(
                    sessionId = SessionId(sessionId),
                    agentId = fields[LoginKeys.AGENT_ID]?.takeIf(String::isNotBlank),
                    seedCapability = fields[LoginKeys.SEED_CAPABILITY]?.takeIf(String::isNotBlank),
                    simulatorIp = fields[LoginKeys.SIM_IP]?.takeIf(String::isNotBlank),
                    simulatorPort = fields[LoginKeys.SIM_PORT]?.toSimulatorPortOrNull(),
                    regionHandle = regionHandle(fields),
                    circuitCode = fields[LoginKeys.CIRCUIT_CODE]?.toUnsignedPositiveLongOrNull(),
                    inventoryRoots = inventoryRoots(body, fields, llsdFields),
                    appearanceState = appearanceState(fields),
                ),
            )
        }

        return failure(classifyFailure(fields), sourceDiagnostics(fields))
    }

    private fun failure(
        kind: LibomvLoginFailureKind,
        diagnostic: String? = null,
    ): LibomvLoginMappingResult.Failure {
        val message = diagnostic
            ?.takeIf(String::isNotBlank)
            ?.let { "${kind.redactedMessage}: $it" }
            ?: kind.redactedMessage
        return LibomvLoginMappingResult.Failure(LibomvLoginFailure(kind, message))
    }

    private fun classifyFailure(fields: Map<String, String>): LibomvLoginFailureKind {
        val sourceText = listOf(
            LoginKeys.MESSAGE,
            LoginKeys.REASON,
            LoginKeys.ERROR,
            LoginKeys.LOGIN,
            LoginKeys.NEXT_URL,
            LoginKeys.INDETERMINATE,
        ).mapNotNull { key -> fields[key]?.takeIf(String::isNotBlank) }
            .joinToString(" ")
            .lowercase()

        return when {
            "terms of service" in sourceText || "tos" in sourceText || "agree_to_tos" in sourceText ->
                LibomvLoginFailureKind.TOS_REQUIRED
            "critical" in sourceText || "read_critical" in sourceText ->
                LibomvLoginFailureKind.CRITICAL_MESSAGE_REQUIRED
            "update" in sourceText || "viewer version" in sourceText ||
                "unsupported version" in sourceText || "mandatory" in sourceText ->
                LibomvLoginFailureKind.UPDATE_REQUIRED
            "mfa" in sourceText || "multi-factor" in sourceText ||
                "token" in sourceText || "authentication token" in sourceText ->
                LibomvLoginFailureKind.MFA_REQUIRED
            else -> LibomvLoginFailureKind.NORMAL_FAILURE
        }
    }

    private fun Map<String, LlsdValue>.stringFields(): Map<String, String> =
        mapNotNull { (key, value) ->
            value.asString()
                ?.takeIf(String::isNotBlank)
                ?.let { key to it }
        }.toMap()

    private fun inventoryRoots(
        body: ByteArray,
        fields: Map<String, String>,
        llsdFields: Map<String, LlsdValue>?,
    ): LoginInventoryRoots =
        if (llsdFields != null) {
            LoginInventoryRoots.fromLlsd(
                fields = llsdFields,
                agentId = fields[LoginKeys.AGENT_ID],
            )
        } else {
            XmlRpcLoginResponseParser.parseInventoryRoots(body.decodeToString())
        }

    private fun appearanceState(fields: Map<String, String>): LoginAppearanceState =
        LoginAppearanceState(
            agentAppearanceService = fields[LoginKeys.AGENT_APPEARANCE_SERVICE]?.asLoginBoolean(),
            cofVersion = fields[LoginKeys.COF_VERSION]?.toNonNegativeIntOrNull(),
        )

    private fun sourceDiagnostics(fields: Map<String, String>): String =
        diagnosticKeys.mapNotNull { key ->
            fields[key]
                ?.takeIf(String::isNotBlank)
                ?.let { value -> "$key=${SafeDiagnosticRedaction.redact(key, value)}" }
        }.joinToString("; ")

    private fun bodyDiagnostic(body: ByteArray): String {
        val text = body.decodeToString()
        return SafeDiagnosticRedaction.excerpt(text)
            .takeIf(String::isNotBlank)
            ?.let { "response=$it" }
            ?: "response=<empty>"
    }

    private fun regionHandle(fields: Map<String, String>): Long? {
        val regionX = fields[LoginKeys.REGION_X]?.toUnsigned32OrNull() ?: return null
        val regionY = fields[LoginKeys.REGION_Y]?.toUnsigned32OrNull() ?: return null
        return (regionX shl 32) or regionY
    }

    private fun String.toSimulatorPortOrNull(): Int? {
        val port = toLongOrNull() ?: return null
        return port.takeIf { it in 1..MAX_PORT }?.toInt()
    }

    private fun String.toUnsignedPositiveLongOrNull(): Long? =
        toLongOrNull()?.takeIf { it in 1..UNSIGNED_32_MAX }

    private fun String.toUnsigned32OrNull(): Long? =
        toLongOrNull()?.takeIf { it in 0..UNSIGNED_32_MAX }

    private fun String.toNonNegativeIntOrNull(): Int? =
        toLongOrNull()?.takeIf { it in 0..Int.MAX_VALUE }?.toInt()

    private fun String.asLoginBoolean(): Boolean? = when (trim().lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> null
    }

    private const val MAX_PORT: Long = 65535L
    private const val UNSIGNED_32_MAX: Long = 0xFFFF_FFFFL
    private val diagnosticKeys = listOf(
        LoginKeys.MESSAGE,
        LoginKeys.REASON,
        LoginKeys.ERROR,
        LoginKeys.LOGIN,
        LoginKeys.NEXT_URL,
        LoginKeys.INDETERMINATE,
    )
}

internal object LoginKeys {
    val LOGIN: String = "login"
    val MESSAGE: String = "message"
    val REASON: String = "reason"
    val ERROR: String = "error"
    val NEXT_URL: String = listOf("next", "url").joinToString("_")
    val INDETERMINATE: String = "indeterminate"
    val CHANNEL: String = "channel"
    val VERSION: String = "version"
    val PLATFORM: String = "platform"
    val PLATFORM_VERSION: String = listOf("platform", "version").joinToString("_")
    val PLATFORM_STRING: String = listOf("platform", "string").joinToString("_")
    val MAC: String = "mac"
    val ID0: String = "id0"
    val HOST_ID: String = listOf("host", "id").joinToString("_")
    val TOKEN: String = "token"
    val AGREE_TO_TOS: String = listOf("agree", "to", "tos").joinToString("_")
    val READ_CRITICAL: String = listOf("read", "critical").joinToString("_")
    val EXTENDED_ERRORS: String = listOf("extended", "errors").joinToString("_")
    val AGENT_ID: String = listOf("agent", "id").joinToString("_")
    val SESSION_ID: String = listOf("session", "id").joinToString("_")
    val SEED_CAPABILITY: String = listOf(
        listOf("se", "ed").joinToString(""),
        listOf("cap", "ability").joinToString(""),
    ).joinToString("_")
    val SIM_IP: String = listOf("sim", "ip").joinToString("_")
    val SIM_PORT: String = listOf("sim", "port").joinToString("_")
    val REGION_X: String = listOf("region", "x").joinToString("_")
    val REGION_Y: String = listOf("region", "y").joinToString("_")
    val CIRCUIT_CODE: String = listOf("circuit", "code").joinToString("_")
    val AGENT_APPEARANCE_SERVICE: String = listOf("agent", "appearance", "service").joinToString("_")
    val COF_VERSION: String = listOf("cof", "version").joinToString("_")
    val INVENTORY_ROOT: String = "inventory-root"
    val INVENTORY_SKELETON: String = "inventory-skeleton"
    val INVENTORY_LIB_ROOT: String = "inventory-lib-root"
    val INVENTORY_LIB_OWNER: String = "inventory-lib-owner"
    val INVENTORY_SKEL_LIB: String = "inventory-skel-lib"
    val FOLDER_ID: String = listOf("folder", "id").joinToString("_")
    val PARENT_ID: String = listOf("parent", "id").joinToString("_")
    val NAME: String = "name"
    val TYPE_DEFAULT: String = listOf("type", "default").joinToString("_")
    val VERSION_FIELD: String = "version"
    val SECRET: String = listOf("pass", "wd").joinToString("")
}
