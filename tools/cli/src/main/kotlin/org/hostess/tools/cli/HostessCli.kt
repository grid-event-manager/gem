package org.hostess.tools.cli

import org.hostess.tools.cli.composition.CliCompositionRoot

object HostessCli {
    @JvmStatic
    fun main(args: Array<String>) {
        CommandRegistry.default(CliCompositionRoot())
            .execute(args.toList(), PrintStreamCliOutput.standard())
    }
}
