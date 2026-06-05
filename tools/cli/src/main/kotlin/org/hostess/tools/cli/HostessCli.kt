package org.hostess.tools.cli

import kotlin.system.exitProcess
import org.hostess.tools.cli.composition.CliCompositionRoot

object HostessCli {
    @JvmStatic
    fun main(args: Array<String>) {
        val exitCode = CommandRegistry.default(CliCompositionRoot())
            .execute(args.toList(), PrintStreamCliOutput.standard())
        exitProcess(exitCode)
    }
}
