package org.hostess.tools.cli.commands

import org.hostess.core.ports.GroupListResult
import org.hostess.tools.cli.CliCommand
import org.hostess.tools.cli.CliOutput
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandResult
import org.hostess.tools.cli.composition.CliCompositionRoot

class ListGroupsCommand(
    private val compositionRoot: CliCompositionRoot,
) : CliCommand {
    override val name: String = "list-groups"

    override fun execute(arguments: CommandArguments, output: CliOutput): CommandResult {
        val mode = arguments.mode()
        val runtime = compositionRoot.runtime(mode)

        return when (val result = runtime.groupDirectoryService.currentGroups(runtime.sessionProvider())) {
            is GroupListResult.Success -> {
                output.line("list-groups ${mode.label()} groups=${result.groups.size}")
                result.groups.forEach { group ->
                    val sendState = if (group.canSendNotices) "sendable" else "cannot-send"
                    output.line("${group.displayName.value} [$sendState]")
                }
                CommandResult.SUCCESS
            }
            is GroupListResult.Failure -> {
                output.line("list-groups ${mode.label()} failed: ${result.failure.redactedMessage ?: "unavailable"}")
                CommandResult.UNAVAILABLE
            }
        }
    }
}
