package org.hostess.tools.cli.commands

import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.ScriptedAgentEvidenceSource
import org.hostess.core.services.SafeDiagnosticRedaction
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
    val operatorObservationReady: Boolean,
    val operatorReceiptStatus: OperatorReceiptStatus? = null,
    val operatorReceiptDetail: String? = null,
    val existingAttachmentName: String?,
    private val operatorReceiptStatusInvalid: Boolean = false,
    private val staleNoticeTotalsOptions: List<String> = emptyList(),
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
            staleNoticeTotalsOptions.forEach { add("unsupported stale option: $it") }
            if (operatorReceiptStatusInvalid) add("operator-receipt-status")
            if (!authorisedLiveSend) add("authorised-live-send")
            if (targetDisplayNames.isEmpty()) add("target display name")
            if (subject.isNullOrBlank()) add("subject")
            if (body.isNullOrBlank()) add("body")
            if (existingAttachmentName.isNullOrBlank()) add("existing-attachment-name")
            if (requiresOperatorObservation() && !operatorObservationReady) add("operator-observation-ready")
        }
        if (proofScope == LiveProofScope.NOTICE_ARCHIVE && targetDisplayNames.isEmpty()) {
            add("target display name")
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
        put("operatorObservationReady", operatorObservationReady.toString())
        put("operatorReceiptStatus", operatorReceiptStatusReportValue())
        redactedOperatorReceiptDetail()?.let { put("operatorReceiptDetail", it) }
        if (proofScope == LiveProofScope.FULL) {
            put("existingAttachmentDisplayName", existingAttachmentName.orEmpty())
        }
    }

    fun validationStatusFields(): Map<String, String> =
        LiveProofStep.statusFields().toMutableMap().also { fields ->
            if (!credentialFile.isNullOrBlank() || credentialHandle.isNullOrBlank()) {
                fields["credentialStatus"] = "blocked"
            }
            if (operatorReceiptStatusInvalid) {
                fields["operatorReceiptStatus"] = "blocked"
            }
            if (requiresOperatorObservation() && !operatorObservationReady) {
                fields["operatorReceiptStatus"] = "blocked"
            }
            fields += loginComplianceStatusFields()
        }

    fun requiresOperatorObservation(): Boolean =
        proofScope == LiveProofScope.FULL && grid.isSecondLifeGrid()

    fun operatorReceiptStatusReportValue(): String =
        when {
            operatorReceiptStatusInvalid -> "blocked"
            !requiresOperatorObservation() -> "not_applicable"
            operatorReceiptStatus != null -> operatorReceiptStatus.reportValue
            operatorObservationReady -> "pending"
            else -> "blocked"
        }

    private fun redactedOperatorReceiptDetail(): String? =
        operatorReceiptDetail
            ?.takeIf(String::isNotBlank)
            ?.let(SafeDiagnosticRedaction::excerpt)

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

    companion object {
        fun from(arguments: CommandArguments): LiveProofInputs {
            val compliance = LoginComplianceArguments(arguments, CommandMode.LIVE)
            val operatorReceiptStatusInput = arguments.option("operator-receipt-status")
            val operatorReceiptStatus = OperatorReceiptStatus.parse(operatorReceiptStatusInput)
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
                operatorObservationReady = arguments.has("operator-observation-ready"),
                operatorReceiptStatus = operatorReceiptStatus,
                operatorReceiptDetail = arguments.option("operator-receipt-detail"),
                existingAttachmentName = arguments.option("existing-attachment-name"),
                operatorReceiptStatusInvalid = operatorReceiptStatusInput != null && operatorReceiptStatus == null,
                staleNoticeTotalsOptions = staleNoticeTotalsOptions(arguments),
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

        private fun staleNoticeTotalsOptions(arguments: CommandArguments): List<String> =
            listOf("ledger", "recipient-count", "recipient-count-source")
                .filter(arguments::has)
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

    private fun String?.isSecondLifeGrid(): Boolean =
        when (this?.trim()?.lowercase()) {
            "agni",
            "second-life",
            "second life",
            "secondlife",
            "sl",
            "main-grid",
            "main grid",
            -> true
            else -> false
    }
}

internal enum class OperatorReceiptStatus(val reportValue: String) {
    ALL_RECEIVED("all_received"),
    PARTIAL("partial"),
    NONE("none"),
    NOT_CHECKED("not_checked"),
    ;

    companion object {
        fun parse(value: String?): OperatorReceiptStatus? =
            entries.firstOrNull { it.reportValue == value?.trim()?.lowercase() }
    }
}

internal enum class LiveProofScope(val wireValue: String) {
    FULL("full"),
    SIMULATOR_PRESENCE("simulator-presence"),
    NOTICE_ARCHIVE("notice-archive"),
    READ_GROUPS("read-groups"),
    LOGIN_ONLY("login-only"),
    INVENTORY_CATALOGUE("inventory-catalogue"),
    UNSUPPORTED("unsupported"),
    ;

    companion object {
        fun parse(value: String?): LiveProofScope = when (value?.lowercase()) {
            null, "", "full" -> FULL
            "simulator-presence" -> SIMULATOR_PRESENCE
            "notice-archive" -> NOTICE_ARCHIVE
            "read-groups" -> READ_GROUPS
            "login-only" -> LOGIN_ONLY
            "inventory-catalogue" -> INVENTORY_CATALOGUE
            else -> UNSUPPORTED
        }
    }
}
