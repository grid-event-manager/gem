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
        val mode = arguments.mode()
        if (mode == CommandMode.FAKE) {
            val runtime = compositionRoot.runtime(mode)
            runtime.proofReportWriter.writeIfRequested(
                reportPath = arguments.option("report"),
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.NOT_RUN,
                inputs = mapOf("mode" to mode.label()),
                blockedReason = "live execution belongs to HS001-A-10",
            )
            output.line("live-proof fake not_run: live execution belongs to HS001-A-10")
            return CommandResult.UNAVAILABLE
        }

        val account = arguments.option("account")
        val credentialHandle = arguments.option("credential-env") ?: arguments.option("credential-file")
        val runtime = compositionRoot.runtime(mode)
        if (account == null || credentialHandle == null) {
            runtime.proofReportWriter.writeIfRequested(
                reportPath = arguments.option("report"),
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.BLOCKED,
                inputs = mapOf("account" to account.orEmpty()),
                blockedReason = "account and credential handle are required",
            )
            output.line("live-proof live blocked: account and credential handle are required")
            output.line("usage: live-proof --mode live --account <label> --credential-env <name> --report <path>")
            return CommandResult.USAGE_ERROR
        }

        runtime.proofReportWriter.writeIfRequested(
            reportPath = arguments.option("report"),
            command = name,
            mode = mode.label(),
            status = ProofReportStatus.BLOCKED,
            inputs = mapOf("account" to account, "credentialHandle" to credentialHandle),
            blockedReason = "live execution belongs to HS001-A-10",
        )
        output.line("live-proof live blocked: live execution belongs to HS001-A-10")
        return CommandResult.UNAVAILABLE
    }
}
