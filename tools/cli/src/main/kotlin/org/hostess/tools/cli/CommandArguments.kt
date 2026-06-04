package org.hostess.tools.cli

class CommandArguments private constructor(
    private val options: Map<String, List<String>>,
    val positional: List<String>,
) {
    fun option(name: String): String? = options[name]?.lastOrNull()

    fun optionValues(name: String): List<String> = options[name].orEmpty()

    fun mode(): CommandMode = CommandMode.parse(option("mode"))

    fun has(name: String): Boolean = options.containsKey(name)

    companion object {
        fun parse(rawArgs: List<String>): CommandArguments {
            val options = linkedMapOf<String, MutableList<String>>()
            val positional = mutableListOf<String>()
            var index = 0

            while (index < rawArgs.size) {
                val token = rawArgs[index]
                if (token.startsWith("--")) {
                    val name = token.removePrefix("--")
                    val next = rawArgs.getOrNull(index + 1)
                    if (next != null && !next.startsWith("--")) {
                        options.getOrPut(name) { mutableListOf() } += next
                        index += 2
                    } else {
                        options.getOrPut(name) { mutableListOf() } += "true"
                        index += 1
                    }
                } else {
                    positional += token
                    index += 1
                }
            }

            return CommandArguments(options, positional)
        }
    }
}

enum class CommandMode {
    FAKE,
    LIVE,
    ;

    companion object {
        fun parse(value: String?): CommandMode = when (value?.lowercase()) {
            null, "fake" -> FAKE
            "live" -> LIVE
            else -> throw IllegalArgumentException("Unknown mode: $value")
        }
    }
}
