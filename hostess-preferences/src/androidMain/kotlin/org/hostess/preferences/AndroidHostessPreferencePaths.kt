package org.hostess.preferences

object AndroidHostessPreferencePaths {
    fun defaultPreferenceFile(appFilesDir: String): String =
        joinPath(appFilesDir, "Hostess", "preferences", "ui.properties")

    private fun joinPath(
        first: String,
        vararg more: String,
    ): String {
        val separator = if ('\\' in first) "\\" else "/"
        return (listOf(first.trimEnd('/', '\\')) + more.map { it.trim('/', '\\') })
            .joinToString(separator)
    }
}
