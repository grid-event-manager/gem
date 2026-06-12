package org.gem.tools.cli

import kotlin.system.exitProcess
import org.gem.tools.cli.composition.CliCompositionRoot

object GemCli {
    @JvmStatic
    fun main(args: Array<String>) {
        val exitCode = CommandRegistry.default(CliCompositionRoot())
            .execute(args.toList(), PrintStreamCliOutput.standard())
        exitProcess(exitCode)
    }
}
