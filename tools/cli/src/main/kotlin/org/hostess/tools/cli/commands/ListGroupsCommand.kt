package org.hostess.tools.cli.commands

import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.SessionLoginResult
import org.hostess.tools.cli.CliCommand
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliCompositionRoot
import org.hostess.tools.cli.report.ProofReportStatus

class ListGroupsCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "list-groups"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        val mode = arguments.mode()
        val request = CommandLoginRequest.from(arguments, mode)
            ?: return usage(output, "missing account or credential handle")
        val complianceArguments = LoginComplianceArguments(arguments, mode)
        val runtime = compositionRoot.runtime(mode)
        val missingCompliance = complianceArguments.missingRequiredFields(forLiveProof = false)
        if (missingCompliance.isNotEmpty()) {
            val reason = "missing login compliance input: ${missingCompliance.joinToString(", ")}"
            runtime.proofReportWriter.writeIfRequested(
                reportPath = arguments.option("report"),
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.BLOCKED,
                statusFields = complianceArguments.statusFields(),
                inputs = mapOf("account" to request.accountLabel.value) + complianceArguments.reportInputs(),
                blockedReason = reason,
            )
            output.line("list-groups ${mode.label()} blocked: $reason")
            return usage(output, reason)
        }
        val session = when (
            val login = runtime.sessionService.login(
                request,
                complianceArguments.request(request.accountLabel.value),
            )
        ) {
            is SessionLoginResult.Success -> login.session
            is SessionLoginResult.Failure -> {
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.FAILED,
                    statusFields = complianceArguments.statusFields(),
                    inputs = mapOf("account" to request.accountLabel.value) + complianceArguments.reportInputs(),
                    blockedReason = login.failure.redactedMessage,
                )
                output.line("list-groups ${mode.label()} failed: ${login.failure.redactedMessage ?: "login unavailable"}")
                return CommandResult.UNAVAILABLE
            }
        }

        return try {
            when (val result = runtime.groupDirectoryService.currentGroups(session)) {
                is GroupListResult.Success -> {
                    runtime.proofReportWriter.writeIfRequested(
                        reportPath = arguments.option("report"),
                        command = name,
                        mode = mode.label(),
                        status = ProofReportStatus.PASSED,
                        inputs = mapOf("mode" to mode.label()),
                        results = result.groups.map { group ->
                            mapOf(
                                "displayName" to group.displayName.value,
                                "canSendNotices" to group.canSendNotices.toString(),
                                "acceptsNotices" to group.acceptsNotices.toString(),
                            )
                        },
                    )
                    output.line("list-groups ${mode.label()} groups=${result.groups.size}")
                    result.groups.forEach { group ->
                        val sendState = if (group.canSendNotices) "sendable" else "cannot-send"
                        output.line("${group.displayName.value} [$sendState]")
                    }
                    CommandResult.SUCCESS
                }
                is GroupListResult.Failure -> {
                    runtime.proofReportWriter.writeIfRequested(
                        reportPath = arguments.option("report"),
                        command = name,
                        mode = mode.label(),
                        status = ProofReportStatus.FAILED,
                        inputs = mapOf("mode" to mode.label()),
                        blockedReason = result.failure.redactedMessage,
                    )
                    output.line("list-groups ${mode.label()} failed: ${result.failure.redactedMessage ?: "unavailable"}")
                    CommandResult.UNAVAILABLE
                }
            }
        } finally {
            runtime.sessionService.logout(session)
        }
    }

    private fun usage(output: CliOutput, reason: String): CommandResult {
        output.line("list-groups usage error: $reason")
        output.line(
            "usage: list-groups --mode fake|live --account <label> --credential-env <name> --report <path> " +
                "--proof-account-attested --scripted-agent-attested --operator <label> --proof-account-label <label>",
        )
        return CommandResult.USAGE_ERROR
    }
}
