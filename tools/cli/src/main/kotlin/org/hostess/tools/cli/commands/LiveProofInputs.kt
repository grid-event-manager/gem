package org.hostess.tools.cli.commands

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentPayloadHandle
import org.hostess.core.domain.AttachmentRequest
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.LocalPosition
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.ScriptedAgentEvidenceSource
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode

internal data class LiveProofInputs(
    val proofScope: LiveProofScope = LiveProofScope.FULL,
    val grid: String?,
    val account: String?,
    val credentialHandle: String?,
    val credentialFile: String? = null,
    val proofAccountAttested: Boolean,
    val scriptedAgentAttested: Boolean,
    val automatedUse: Boolean,
    val operator: String?,
    val proofAccountLabel: String?,
    val targetDisplayNames: List<String>,
    val subject: String?,
    val body: String?,
    val authorisedLiveSend: Boolean,
    val existingAttachmentKind: String?,
    val existingAttachmentId: String?,
    val landmarkVenue: String?,
    val landmarkRegionId: String?,
    val landmarkLocalPosition: String?,
    val textureFileName: String?,
    val texturePayloadHandle: String?,
    val textureDigest: String?,
    val bulkLimit: Int?,
    val bulkDelayMs: Long?,
    val cleanupMode: String?,
    val retentionNote: String?,
) {
    fun missingRequiredFields(): List<String> = buildList {
        if (proofScope == LiveProofScope.UNSUPPORTED) {
            add("proof-scope")
            return@buildList
        }
        if (grid.isNullOrBlank()) add("grid")
        if (account.isNullOrBlank()) add("account")
        if (!credentialFile.isNullOrBlank()) {
            add("unsupported credential route")
        } else if (credentialHandle.isNullOrBlank()) {
            add("credential-env")
        }
        if (!proofAccountAttested) add("proof-account-attested")
        if (automatedUse && !scriptedAgentAttested) add("scripted-agent-attested")
        if (operator.isNullOrBlank()) add("operator")
        if (proofAccountLabel.isNullOrBlank()) add("proof-account-label")
        if (proofScope == LiveProofScope.FULL) {
            if (!authorisedLiveSend) add("authorised-live-send")
            if (targetDisplayNames.isEmpty()) add("target display name")
            if (subject.isNullOrBlank()) add("subject")
            if (body.isNullOrBlank()) add("body")
        }
    }

    fun toReportInputs(mode: CommandMode): Map<String, String> = buildMap {
        put("mode", mode.label())
        if (proofScope == LiveProofScope.READ_GROUPS) {
            put("proofScope", proofScope.wireValue)
        }
        put("grid", grid.orEmpty())
        put("account", account.orEmpty())
        put("authHandlePresent", (!credentialHandle.isNullOrBlank()).toString())
        if (!credentialFile.isNullOrBlank()) {
            put("credentialFilePresent", "true")
        }
        putAll(loginComplianceReportInputs())
        put("targetCount", targetDisplayNames.size.toString())
        put("targetDisplayNames", targetDisplayNames.joinToString("|"))
        put("subject", subject.orEmpty())
        put("bodyLength", body.orEmpty().length.toString())
        put("authorisedLiveSend", authorisedLiveSend.toString())
        existingAttachmentKind?.let { put("existingAttachmentKind", it) }
        existingAttachmentId?.let { put("existingAttachmentId", it) }
        landmarkVenue?.let { put("landmarkVenue", it) }
        landmarkRegionId?.let { put("landmarkRegionId", it) }
        landmarkLocalPosition?.let { put("landmarkLocalPos", it) }
        textureFileName?.let { put("textureFileName", safeTextureFileName()) }
        textureDigest?.let { put("textureDigest", it) }
        bulkLimit?.let { put("bulkLimit", it.toString()) }
        bulkDelayMs?.let { put("bulkDelayMs", it.toString()) }
        cleanupMode?.let { put("cleanupMode", it) }
        retentionNote?.let { put("retentionNote", it) }
    }

    fun landmarkRequest(): AttachmentRequest? =
        existingRequest(AttachmentKind.LANDMARK) ?: createdLandmarkRequest()

    fun textureRequest(): AttachmentRequest? =
        existingRequest(AttachmentKind.TEXTURE) ?: uploadTextureRequest()

    fun cleanupModeValue(): String = cleanupMode?.lowercase() ?: "delete-created"

    fun validationStatusFields(): Map<String, String> =
        LiveProofStep.statusFields().toMutableMap().also { fields ->
            if (!credentialFile.isNullOrBlank() || credentialHandle.isNullOrBlank()) {
                fields["credentialStatus"] = "blocked"
            }
            fields += loginComplianceStatusFields()
        }

    fun loginComplianceRequest(): LoginComplianceRequest = LoginComplianceRequest(
        proofAccountAttested = proofAccountAttested,
        scriptedAgentAttested = scriptedAgentAttested,
        automatedUse = automatedUse,
        operatorLabel = OperatorLabel(operator.orEmpty().trim()),
        proofAccountLabel = proofAccountLabel.orEmpty().trim(),
        evidenceSource = if (scriptedAgentAttested) {
            ScriptedAgentEvidenceSource.OPERATOR_ATTESTED
        } else {
            ScriptedAgentEvidenceSource.ABSENT
        },
    )

    fun loginComplianceStatusFields(): Map<String, String> {
        val evidencePresent = scriptedAgentAttested
        val complete = proofAccountAttested && evidencePresent && !operator.isNullOrBlank() && !proofAccountLabel.isNullOrBlank()
        return mapOf(
            "loginComplianceStatus" to if (complete) "passed" else "blocked",
            "proofAccountStatus" to if (proofAccountAttested) "passed" else "blocked",
            "scriptedAgentStatus" to if (evidencePresent) "passed" else "blocked",
            "operatorStatus" to if (!operator.isNullOrBlank()) "passed" else "blocked",
        )
    }

    private fun existingRequest(kind: AttachmentKind): AttachmentRequest? {
        val requestedKind = when (existingAttachmentKind?.lowercase()) {
            "landmark" -> AttachmentKind.LANDMARK
            "texture" -> AttachmentKind.TEXTURE
            else -> return null
        }
        if (requestedKind != kind) {
            return null
        }
        val itemId = existingAttachmentId?.takeIf(String::isNotBlank) ?: return null
        return ExistingInventoryAttachment(kind, InventoryItemId(itemId))
    }

    private fun createdLandmarkRequest(): AttachmentRequest? {
        val venue = landmarkVenue?.takeIf(String::isNotBlank) ?: return null
        val regionId = landmarkRegionId?.takeIf(String::isNotBlank) ?: return null
        val position = localPosition() ?: return null
        return CreateLandmarkAttachment(
            venueLabel = venue,
            regionId = regionId,
            localPosition = position,
        )
    }

    private fun uploadTextureRequest(): AttachmentRequest? {
        val handle = texturePayloadHandle?.takeIf(String::isNotBlank) ?: return null
        val digest = textureDigest?.takeIf(String::isNotBlank) ?: return null
        return UploadTextureAttachment(
            fileName = safeTextureFileName(),
            contentDigest = digest,
            payloadHandle = AttachmentPayloadHandle(handle),
        )
    }

    private fun localPosition(): LocalPosition? {
        val parts = landmarkLocalPosition?.split(",")?.map(String::trim) ?: return null
        if (parts.size != 3) {
            return null
        }
        val x = parts[0].toDoubleOrNull() ?: return null
        val y = parts[1].toDoubleOrNull() ?: return null
        val z = parts[2].toDoubleOrNull() ?: return null
        return LocalPosition(x, y, z)
    }

    private fun safeTextureFileName(): String =
        textureFileName
            ?.replace('\\', '/')
            ?.substringAfterLast('/')
            ?.takeIf(String::isNotBlank)
            ?: "texture-upload"

    companion object {
        fun from(arguments: CommandArguments): LiveProofInputs {
            val compliance = LoginComplianceArguments(arguments, CommandMode.LIVE)
            return LiveProofInputs(
                proofScope = LiveProofScope.parse(arguments.option("proof-scope")),
                grid = arguments.option("grid"),
                account = arguments.option("account"),
                credentialHandle = arguments.option("credential-env"),
                credentialFile = arguments.option("credential-file"),
                proofAccountAttested = compliance.proofAccountAttested(),
                scriptedAgentAttested = compliance.scriptedAgentAttested(),
                automatedUse = compliance.automatedUse(defaultAutomatedUse = true),
                operator = compliance.operatorLabel(),
                proofAccountLabel = compliance.proofAccountLabel(),
                targetDisplayNames = targetDisplayNames(arguments),
                subject = arguments.option("subject"),
                body = arguments.option("body"),
                authorisedLiveSend = arguments.has("authorised-live-send"),
                existingAttachmentKind = arguments.option("existing-attachment-kind"),
                existingAttachmentId = arguments.option("existing-attachment-id"),
                landmarkVenue = arguments.option("landmark-venue"),
                landmarkRegionId = arguments.option("landmark-region-id"),
                landmarkLocalPosition = arguments.option("landmark-local-pos"),
                textureFileName = arguments.option("texture-file-name"),
                texturePayloadHandle = arguments.option("texture-payload-handle"),
                textureDigest = arguments.option("texture-digest"),
                bulkLimit = arguments.option("bulk-limit")?.toIntOrNull(),
                bulkDelayMs = arguments.option("bulk-delay-ms")?.toLongOrNull(),
                cleanupMode = arguments.option("cleanup-mode"),
                retentionNote = arguments.option("retention-note"),
            )
        }

        private fun targetDisplayNames(arguments: CommandArguments): List<String> =
            arguments.optionValues("target")
                .map(String::trim)
                .filter(String::isNotBlank)
                .ifEmpty {
                    arguments.option("group")
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.let(::listOf)
                        .orEmpty()
                }
    }

    private fun loginComplianceReportInputs(): Map<String, String> =
        mapOf(
            "proofAccountLabel" to proofAccountLabel.orEmpty(),
            "operator" to operator.orEmpty(),
            "proofAccountAttested" to proofAccountAttested.toString(),
            "scriptedAgentAttested" to scriptedAgentAttested.toString(),
            "automatedUse" to automatedUse.toString(),
            "scriptedAgentEvidenceSource" to if (scriptedAgentAttested) {
                ScriptedAgentEvidenceSource.OPERATOR_ATTESTED.reportValue
            } else {
                ScriptedAgentEvidenceSource.ABSENT.reportValue
            },
        )
}

internal enum class LiveProofScope(val wireValue: String) {
    FULL("full"),
    READ_GROUPS("read-groups"),
    UNSUPPORTED("unsupported"),
    ;

    companion object {
        fun parse(value: String?): LiveProofScope = when (value?.lowercase()) {
            null, "", "full" -> FULL
            "read-groups" -> READ_GROUPS
            else -> UNSUPPORTED
        }
    }
}
