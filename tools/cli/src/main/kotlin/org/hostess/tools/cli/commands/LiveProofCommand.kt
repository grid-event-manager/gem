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
            usage(output)
            return CommandResult.USAGE_ERROR
        }

        val inputs = LiveProofInputs.from(arguments)
        val runtime = compositionRoot.runtime(mode)
        val missingInputs = inputs.missingRequiredFields()
        if (missingInputs.isNotEmpty()) {
            val reason = "missing required live proof input: ${missingInputs.joinToString(", ")}"
            runtime.proofReportWriter.writeIfRequested(
                reportPath = reportPath,
                command = name,
                mode = mode.label(),
                status = ProofReportStatus.BLOCKED,
                inputs = inputs.toReportInputs(mode),
                results = LiveProofStep.blockedPlan("validate-inputs", reason).map(LiveProofStep::toReportMap),
                blockedReason = reason,
            )
            output.line("live-proof live blocked: $reason")
            usage(output)
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
                inputs = inputs.toReportInputs(mode),
                results = results.map(LiveProofStep::toReportMap),
                blockedReason = reason,
            )
            output.line("live-proof live blocked: $reason")
            return CommandResult.UNAVAILABLE
        }

        return LiveProofRunner(runtime, inputs, reportPath, output, commandName = name).run()
    }

    private fun usage(output: CliOutput) {
        output.line(
            "usage: live-proof --report <path> --authorised-live-send --grid <name> " +
                "--account <label> --credential-env <name> --target <display-name> --subject <subject> --body <body>",
        )
    }
}
