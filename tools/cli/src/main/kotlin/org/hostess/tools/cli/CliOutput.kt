package org.hostess.tools.cli

import java.io.PrintStream

interface CliOutput {
    fun line(value: String)
}

class PrintStreamCliOutput(
    private val stream: PrintStream,
) : CliOutput {
    override fun line(value: String) {
        stream.print(value)
        stream.print(System.lineSeparator())
    }

    companion object {
        fun standard(): PrintStreamCliOutput = PrintStreamCliOutput(System.out)
    }
}
