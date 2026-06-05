package org.hostess.tools.cli.commands

import org.hostess.core.domain.LoginComplianceRequest
import org.hostess.core.domain.OperatorLabel
import org.hostess.core.domain.ScriptedAgentEvidenceSource
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode

internal class LoginComplianceArguments(
    private val arguments: CommandArguments,
    private val mode: CommandMode,
) {
    fun missingRequiredFields(forLiveProof: Boolean): List<String> {
        if (mode == CommandMode.FAKE) {
            return emptyList()
        }
        val values = parsed(defaultAutomatedUse = forLiveProof)
        return buildList {
            if (!values.proofAccountAttested) add("proof-account-attested")
            if (values.automatedUse && !values.scriptedAgentAttested) add("scripted-agent-attested")
            if (values.operatorLabel.isNullOrBlank()) add("operator")
            if (values.proofAccountLabel.isNullOrBlank()) add("proof-account-label")
        }
    }

    fun request(
        accountLabel: String?,
        defaultAutomatedUse: Boolean = false,
    ): LoginComplianceRequest {
        if (mode == CommandMode.FAKE) {
            return fakeRequest()
        }
        val values = parsed(defaultAutomatedUse)
        return LoginComplianceRequest(
            proofAccountAttested = values.proofAccountAttested,
            automatedUse = values.automatedUse,
            scriptedAgentAttested = values.scriptedAgentAttested,
            operatorLabel = OperatorLabel(values.operatorLabel.orEmpty().trim()),
            proofAccountLabel = values.proofAccountLabel.orEmpty().trim(),
            evidenceSource = values.evidenceSource,
        )
    }

    fun reportInputs(defaultAutomatedUse: Boolean = false): Map<String, String> {
        val values = parsed(defaultAutomatedUse)
        return mapOf(
            "proofAccountLabel" to values.proofAccountLabel.orEmpty(),
            "operator" to values.operatorLabel.orEmpty(),
            "proofAccountAttested" to values.proofAccountAttested.toString(),
            "scriptedAgentAttested" to values.scriptedAgentAttested.toString(),
            "automatedUse" to values.automatedUse.toString(),
            "scriptedAgentEvidenceSource" to values.evidenceSource.reportValue,
        )
    }

    fun statusFields(defaultAutomatedUse: Boolean = false): Map<String, String> {
        val values = parsed(defaultAutomatedUse)
        val missing = missingRequiredFields(forLiveProof = defaultAutomatedUse)
        val evidencePresent = values.evidenceSource != ScriptedAgentEvidenceSource.ABSENT
        val complianceStatus = if (missing.isEmpty() && evidencePresent) "passed" else "blocked"
        return mapOf(
            "loginComplianceStatus" to complianceStatus,
            "proofAccountStatus" to if (values.proofAccountAttested) "passed" else "blocked",
            "scriptedAgentStatus" to if (evidencePresent) "passed" else "blocked",
            "operatorStatus" to if (!values.operatorLabel.isNullOrBlank()) "passed" else "blocked",
        )
    }

    fun proofAccountAttested(): Boolean = parsed(defaultAutomatedUse = false).proofAccountAttested

    fun scriptedAgentAttested(): Boolean = parsed(defaultAutomatedUse = false).scriptedAgentAttested

    fun automatedUse(defaultAutomatedUse: Boolean): Boolean = parsed(defaultAutomatedUse).automatedUse

    fun operatorLabel(): String? = parsed(defaultAutomatedUse = false).operatorLabel

    fun proofAccountLabel(): String? = parsed(defaultAutomatedUse = false).proofAccountLabel

    private fun parsed(defaultAutomatedUse: Boolean): ParsedComplianceValues {
        if (mode == CommandMode.FAKE) {
            return ParsedComplianceValues(
                proofAccountAttested = true,
                automatedUse = false,
                scriptedAgentAttested = true,
                operatorLabel = "fake-operator",
                proofAccountLabel = "fake-proof-account",
            )
        }
        val scriptedAgentAttested = booleanOption("scripted-agent-attested", default = false)
        return ParsedComplianceValues(
            proofAccountAttested = booleanOption("proof-account-attested", default = false),
            automatedUse = booleanOption("automated-use", default = defaultAutomatedUse),
            scriptedAgentAttested = scriptedAgentAttested,
            operatorLabel = arguments.option("operator")?.trim(),
            proofAccountLabel = arguments.option("proof-account-label")?.trim(),
        )
    }

    private fun booleanOption(name: String, default: Boolean): Boolean =
        arguments.option(name)?.let { value ->
            when (value.lowercase()) {
                "true" -> true
                "false" -> false
                else -> throw IllegalArgumentException("$name must be true or false")
            }
        } ?: default

    private fun fakeRequest(): LoginComplianceRequest = LoginComplianceRequest(
        proofAccountAttested = true,
        scriptedAgentAttested = true,
        automatedUse = false,
        operatorLabel = OperatorLabel("fake-operator"),
        proofAccountLabel = "fake-proof-account",
        evidenceSource = ScriptedAgentEvidenceSource.OPERATOR_ATTESTED,
    )

    private data class ParsedComplianceValues(
        val proofAccountAttested: Boolean,
        val automatedUse: Boolean,
        val scriptedAgentAttested: Boolean,
        val operatorLabel: String?,
        val proofAccountLabel: String?,
    ) {
        val evidenceSource: ScriptedAgentEvidenceSource =
            if (scriptedAgentAttested) ScriptedAgentEvidenceSource.OPERATOR_ATTESTED else ScriptedAgentEvidenceSource.ABSENT
    }
}
