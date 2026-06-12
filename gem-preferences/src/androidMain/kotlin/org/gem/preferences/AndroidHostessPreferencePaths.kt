package org.gem.preferences

object AndroidHostessPreferencePaths {
    fun defaultPreferenceFile(appFilesDir: String): String =
        joinPath(defaultPreferenceDirectory(appFilesDir), "ui.properties")

    fun defaultLastLoginProfileFile(appFilesDir: String): String =
        joinPath(defaultPreferenceDirectory(appFilesDir), "last-login-profile.txt")

    private fun defaultPreferenceDirectory(appFilesDir: String): String =
        joinPath(appFilesDir, "Hostess", "preferences")

    private fun joinPath(
        first: String,
        vararg more: String,
    ): String {
        val separator = if ('\\' in first) "\\" else "/"
        return (listOf(first.trimEnd('/', '\\')) + more.map { it.trim('/', '\\') })
            .joinToString(separator)
    }
}
