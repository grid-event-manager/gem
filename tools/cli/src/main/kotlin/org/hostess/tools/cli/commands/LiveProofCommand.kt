package org.hostess.tools.cli.commands

import org.hostess.tools.cli.CliCommand
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliCompositionRoot
import org.hostess.tools.cli.report.ProofReportStatus

class LiveProofCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "live-proof"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        val mode = CommandMode.parse(arguments.option("mode") ?: "live")
        if (mode == CommandMode.FAKE) {
            val runtime = compositionRoot.runtime(mode)
            runtime.proofReportWriter.writeIfRequested(
                reportPath = arguments.option("report"),
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.NOT_RUN,
                statusFields = LiveProofStep.statusFields(),
                inputs = mapOf("mode" to mode.label()),
                results = LiveProofStep.notRunPlan("fake mode cannot satisfy live proof").map(LiveProofStep::toReportMap),
                blockedReason = "fake mode cannot satisfy live proof",
            )
            output.line("live-proof fake not_run: fake mode cannot satisfy live proof")
            return CommandResult.UNAVAILABLE
        }

        val reportPath = arguments.option("report")
        if (reportPath == null) {
            output.line("live-proof live blocked: report path is required")
            usage(output, LiveProofScope.parse(arguments.option("proof-scope")))
            return CommandResult.USAGE_ERROR
        }

        val inputs = LiveProofInputs.from(arguments)
        val runtime = compositionRoot.runtime(mode, inputs.noticeLedgerPath)
        val missingInputs = inputs.missingRequiredFields()
        if (missingInputs.isNotEmpty()) {
            val reason = "missing required live proof input: ${missingInputs.joinToString(", ")}"
            runtime.proofReportWriter.writeIfRequested(
                reportPath = reportPath,
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.BLOCKED,
                statusFields = inputs.validationStatusFields(),
                inputs = inputs.toReportInputs(mode),
                results = LiveProofStep.blockedPlan("validate-inputs", reason).map(LiveProofStep::toReportMap),
                blockedReason = reason,
            )
            output.line("live-proof live blocked: $reason")
            usage(output, inputs.proofScope)
            return CommandResult.USAGE_ERROR
        }

        if (!runtime.protocolAvailable) {
            val reason = "protocol bootstrap unavailable after HS001-A-07; live grid proof not attempted"
            val results = listOf(LiveProofStep.passed("validate-inputs")) +
                LiveProofStep.blockedPlan("login", reason)
            runtime.proofReportWriter.writeIfRequested(
                reportPath = reportPath,
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.BLOCKED,
                statusFields = bootstrapBlockedStatusFields(inputs),
                inputs = inputs.toReportInputs(mode),
                results = results.map(LiveProofStep::toReportMap),
                blockedReason = reason,
            )
            output.line("live-proof live blocked: $reason")
            return CommandResult.UNAVAILABLE
        }

        return LiveProofRunner(runtime, inputs, reportPath, output, commandName = name).run()
    }

    private fun bootstrapBlockedStatusFields(inputs: LiveProofInputs): Map<String, String> =
        when (inputs.proofScope) {
            LiveProofScope.READ_GROUPS, LiveProofScope.LOGIN_ONLY, LiveProofScope.INVENTORY_CATALOGUE ->
                LiveProofStep.statusFields().toMutableMap().also {
                    it["credentialStatus"] = "blocked"
                    it["loginStatus"] = "runtime_gap"
                    it += inputs.loginComplianceStatusFields()
                }
            LiveProofScope.FULL, LiveProofScope.UNSUPPORTED -> LiveProofStep.statusFields("blocked").toMutableMap().also {
                it += inputs.loginComplianceStatusFields()
                it += inputs.noticeComplianceArguments().reportStatusFields(null)
            }
        }

    private fun usage(output: CliOutput, scope: LiveProofScope) {
        when (scope) {
            LiveProofScope.READ_GROUPS -> output.line(
                "usage: live-proof --mode live --proof-scope read-groups --report <path> " +
                    "--grid <name> --account <label> --credential-env <name> --proof-account-attested " +
                    "--scripted-agent-attested --operator <label> --proof-account-label <label>",
            )
            LiveProofScope.LOGIN_ONLY -> output.line(
                "usage: live-proof --mode live --proof-scope login-only --report <path> --grid <name> " +
                    "--account <label> --credential-env <name> --proof-account-attested --scripted-agent-attested " +
                    "--operator <label> --proof-account-label <label> [--automated-use true]",
            )
            LiveProofScope.INVENTORY_CATALOGUE -> output.line(
                "usage: live-proof --mode live --proof-scope inventory-catalogue --report <path> " +
                    "--grid <name> --account <label> --credential-env <name> --proof-account-attested " +
                    "--scripted-agent-attested --operator <label> --proof-account-label <label>",
            )
            LiveProofScope.FULL, LiveProofScope.UNSUPPORTED -> output.line(
                "usage: live-proof --report <path> --authorised-live-send --grid <name> --account <label> " +
                    "--credential-env <name> --proof-account-attested --scripted-agent-attested " +
                    "--operator <label> --proof-account-label <label> --target <display-name> " +
                    "--subject <subject> --body <body> --existing-attachment-name <display-name> " +
                    "--recipient-count <display-name=count> " +
                    "--recipient-count-source operator-acknowledged|authoritative --ledger <path>",
            )
        }
    }
}
