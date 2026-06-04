package org.hostess.tools.cli.commands

import org.hostess.core.domain.AccountLabel
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.LoginRequest
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
        val request = loginRequest(arguments, mode)
            ?: return usage(output, "missing account or credential handle")

        val runtime = compositionRoot.runtime(mode)
        return when (val result = runtime.sessionService.login(request)) {
            is SessionLoginResult.Success -> {
                runtime.proofReportWriter.writeIfRequested(
                    reportPath = arguments.option("report"),
                    command = name,
                    mode = mode.label(),
                    status = ProofReportStatus.PASSED,
                    inputs = mapOf("account" to request.accountLabel.value),
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
                    inputs = mapOf("account" to request.accountLabel.value),
                    results = emptyList(),
                    blockedReason = result.failure.redactedMessage,
                )
                output.line("login ${mode.label()} failed: ${result.failure.redactedMessage ?: "unavailable"}")
                CommandResult.UNAVAILABLE
            }
        }
    }

    private fun loginRequest(arguments: CommandArguments, mode: CommandMode): LoginRequest? {
        val account = arguments.option("account") ?: if (mode == CommandMode.FAKE) "fake-account" else return null
        val credentialHandle = arguments.option("credential-env")
            ?: arguments.option("credential-file")
            ?: if (mode == CommandMode.FAKE) "fake-credential" else return null
        return LoginRequest(AccountLabel(account), CredentialHandle(credentialHandle))
    }

    private fun usage(output: CliOutput, reason: String): CommandResult {
        output.line("login usage error: $reason")
        output.line("usage: login --mode fake|live --account <label> --credential-env <name> --report <path>")
        return CommandResult.USAGE_ERROR
    }
}

internal fun CommandMode.label(): String = name.lowercase()
