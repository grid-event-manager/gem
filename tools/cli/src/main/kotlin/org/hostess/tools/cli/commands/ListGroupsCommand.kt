package org.hostess.tools.cli.commands

import org.hostess.core.ports.GroupListResult
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
        val runtime = compositionRoot.runtime(mode)

        return when (val result = runtime.groupDirectoryService.currentGroups(runtime.sessionProvider())) {
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
    }
}
