package org.hostess.tools.cli.commands

import org.hostess.core.ports.SessionLoginResult
import org.hostess.tools.cli.CliCommand
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliCompositionRoot
import org.hostess.tools.cli.report.ProofReportStatus

class LoginCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "login"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        val mode = arguments.mode()
        val request = CommandLoginRequest.from(arguments, mode)
            ?: return usage(output, "missing account or credential handle")
        val complianceArguments = LoginComplianceArguments(arguments, mode)
        val missingCompliance = complianceArguments.missingRequiredFields(forLiveProof = false)
        val runtime = compositionRoot.runtime(mode)
        if (missingCompliance.isNotEmpty()) {
            val reason = "missing login compliance input: ${missingCompliance.joinToString(", ")}"
            runtime.proofReportWriter.writeIfRequested(
                reportPath = arguments.option("report"),
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.BLOCKED,
                statusFields = complianceArguments.statusFields(),
                inputs = mapOf("account" to request.accountLabel.value) + complianceArguments.reportInputs(),
                results = emptyList(),
                blockedReason = reason,
            )
            output.line("login ${mode.label()} blocked: $reason")
            return usage(output, reason)
        }

        val compliance = complianceArguments.request(request.accountLabel.value)
        return when (val result = runtime.sessionService.login(request, compliance)) {
            is SessionLoginResult.Success -> {
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.PASSED,
                    statusFields = complianceArguments.statusFields(),
                    inputs = mapOf("account" to request.accountLabel.value) + complianceArguments.reportInputs(),
                    results = listOf(mapOf("session" to "created")),
                )
                output.line("login ${mode.label()} ready for ${result.session.accountLabel.value}")
                CommandResult.SUCCESS
            }
            is SessionLoginResult.Failure -> {
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = complianceArguments.statusFields(),
                    inputs = mapOf("account" to request.accountLabel.value) + complianceArguments.reportInputs(),
                    results = emptyList(),
                    blockedReason = result.failure.redactedMessage,
                )
                output.line("login ${mode.label()} failed: ${result.failure.redactedMessage ?: "unavailable"}")
                CommandResult.UNAVAILABLE
            }
        }
    }

    private fun usage(output: CliOutput, reason: String): CommandResult {
        output.line("login usage error: $reason")
        output.line(
            "usage: login --mode fake|live --account <label> --credential-env <name> --report <path> " +
                "--proof-account-attested --scripted-agent-attested --operator <label> " +
                "--proof-account-label <label> --automated-use true|false",
        )
        return CommandResult.USAGE_ERROR
    }
}

internal fun CommandMode.label(): String = name.lowercase()
