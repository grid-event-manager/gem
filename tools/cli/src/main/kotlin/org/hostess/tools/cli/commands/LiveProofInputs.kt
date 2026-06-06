package org.hostess.tools.cli.commands

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.ScriptedAgentEvidenceSource
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
    val bulkLimit: Int?,
    val bulkDelayMs: Long?,
    val cleanupMode: String?,
    val retentionNote: String?,
    val recipientCountValues: List<String> = emptyList(),
    val recipientCountSource: String? = null,
    val noticeLedgerPath: String? = null,
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
        if (!scriptedAgentAttested) add("scripted-agent-attested")
        if (operator.isNullOrBlank()) add("operator")
        if (proofAccountLabel.isNullOrBlank()) add("proof-account-label")
        if (proofScope == LiveProofScope.FULL) {
            if (!authorisedLiveSend) add("authorised-live-send")
            if (targetDisplayNames.isEmpty()) add("target display name")
            if (subject.isNullOrBlank()) add("subject")
            if (body.isNullOrBlank()) add("body")
            addAll(noticeComplianceArguments().missingRequiredFields(sendMayOccur = true))
        }
    }

    fun toReportInputs(mode: CommandMode): Map<String, String> = buildMap {
        put("mode", mode.label())
        put("proofScope", proofScope.wireValue)
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
        if (proofScope == LiveProofScope.FULL) {
            putAll(noticeComplianceArguments().reportInputs())
        }
        existingAttachmentKind?.let { put("existingAttachmentKind", it) }
        existingAttachmentId?.let { put("existingAttachmentId", it) }
        bulkLimit?.let { put("bulkLimit", it.toString()) }
        bulkDelayMs?.let { put("bulkDelayMs", it.toString()) }
        cleanupMode?.let { put("cleanupMode", it) }
        retentionNote?.let { put("retentionNote", it) }
    }

    fun existingAttachmentRequest(): ExistingInventoryAttachment? {
        val kind = when (existingAttachmentKind?.lowercase()) {
            "landmark" -> AttachmentKind.LANDMARK
            "texture" -> AttachmentKind.TEXTURE
            else -> return null
        }
        val itemId = existingAttachmentId?.takeIf(String::isNotBlank) ?: return null
        return ExistingInventoryAttachment(kind, InventoryItemId(itemId))
    }

    fun cleanupModeValue(): String = cleanupMode?.lowercase() ?: "delete-created"

    fun validationStatusFields(): Map<String, String> =
        LiveProofStep.statusFields().toMutableMap().also { fields ->
            if (!credentialFile.isNullOrBlank() || credentialHandle.isNullOrBlank()) {
                fields["credentialStatus"] = "blocked"
            }
            fields += loginComplianceStatusFields()
            if (proofScope == LiveProofScope.FULL) {
                fields += noticeComplianceArguments().reportStatusFields(null)
            }
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

    fun noticeComplianceArguments(): NoticeComplianceArguments =
        NoticeComplianceArguments.fromValues(
            mode = CommandMode.LIVE,
            operator = operator,
            recipientCountValues = recipientCountValues,
            recipientCountSource = recipientCountSource,
            ledgerPath = noticeLedgerPath,
        )

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
                bulkLimit = arguments.option("bulk-limit")?.toIntOrNull(),
                bulkDelayMs = arguments.option("bulk-delay-ms")?.toLongOrNull(),
                cleanupMode = arguments.option("cleanup-mode"),
                retentionNote = arguments.option("retention-note"),
                recipientCountValues = arguments.optionValues("recipient-count"),
                recipientCountSource = arguments.option("recipient-count-source"),
                noticeLedgerPath = arguments.option("ledger"),
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
    LOGIN_ONLY("login-only"),
    INVENTORY_CATALOGUE("inventory-catalogue"),
    UNSUPPORTED("unsupported"),
    ;

    companion object {
        fun parse(value: String?): LiveProofScope = when (value?.lowercase()) {
            null, "", "full" -> FULL
            "read-groups" -> READ_GROUPS
            "login-only" -> LOGIN_ONLY
            "inventory-catalogue" -> INVENTORY_CATALOGUE
            else -> UNSUPPORTED
        }
    }
}
