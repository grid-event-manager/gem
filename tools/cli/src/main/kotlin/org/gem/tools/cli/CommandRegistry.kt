package org.gem.tools.cli

import org.gem.tools.cli.commands.ListGroupsCommand
import org.gem.tools.cli.commands.LiveProofCommand
import org.gem.tools.cli.commands.LoginCommand
import org.gem.tools.cli.commands.SendNoticeCommand
import org.gem.tools.cli.composition.CliCompositionRoot

class CommandRegistry(
    commands: List<CliCommand>,
) {
    private val commandsByName: Map<String, CliCommand> = commands.associateBy { it.name }

    val commandNames: Set<String>
        get() = commandsByName.keys.toSortedSet()

    fun execute(rawArgs: List<String>, output: CliOutput): Int {
        val commandName = rawArgs.firstOrNull()
        val command = commandsByName[commandName]
        if (command == null) {
            output.line("available commands: ${commandNames.joinToString(", ")}")
            return CommandResult.USAGE_ERROR.exitCode
        }

        return try {
            command.execute(CommandArguments.parse(rawArgs.drop(1)), output).exitCode
        } catch (exception: IllegalArgumentException) {
            output.line("usage error: ${exception.message}")
            CommandResult.USAGE_ERROR.exitCode
        }
    }

    companion object {
        fun default(compositionRoot: CliCompositionRoot): CommandRegistry = CommandRegistry(
            listOf(
                LoginCommand(compositionRoot),
                ListGroupsCommand(compositionRoot),
                SendNoticeCommand(compositionRoot),
                LiveProofCommand(compositionRoot),
            ),
        )
    }
}

interface CliCommand {
    val name: String

    fun execute(arguments: CommandArguments, output: CliOutput): CommandResult
}

data class CommandResult(val exitCode: Int) {
    companion object {
        val SUCCESS: CommandResult = CommandResult(0)
        val USAGE_ERROR: CommandResult = CommandResult(2)
        val UNAVAILABLE: CommandResult = CommandResult(3)
    }
}
