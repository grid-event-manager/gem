package org.hostess.tools.cli.commands

import org.hostess.tools.cli.CliCommand
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliCompositionRoot

class LiveProofCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "live-proof"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        val mode = arguments.mode()
        if (mode == CommandMode.FAKE) {
            compositionRoot.runtime(mode)
            output.line("live-proof fake not_run: live execution belongs to HS001-A-10")
            return CommandResult.UNAVAILABLE
        }

        val account = arguments.option("account")
        val credentialHandle = arguments.option("credential-env") ?: arguments.option("credential-file")
        if (account == null || credentialHandle == null) {
            output.line("live-proof live blocked: account and credential handle are required")
            output.line("usage: live-proof --mode live --account <label> --credential-env <name>")
            return CommandResult.USAGE_ERROR
        }

        compositionRoot.runtime(mode)
        output.line("live-proof live blocked: live execution belongs to HS001-A-10")
        return CommandResult.UNAVAILABLE
    }
}
