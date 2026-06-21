package org.gem.preferences

object AndroidGemPreferencePaths {
    fun defaultPreferenceFile(appFilesDir: String): String =
        joinPath(defaultPreferenceDirectory(appFilesDir), "ui.properties")

    fun defaultLastLoginProfileFile(appFilesDir: String): String =
        joinPath(defaultPreferenceDirectory(appFilesDir), "last-login-profile.txt")

    fun defaultAppearanceProfileFile(appFilesDir: String): String =
        joinPath(defaultPreferenceDirectory(appFilesDir), "appearance-profiles.properties")

    fun defaultLanguagePreferenceFile(appFilesDir: String): String =
        joinPath(defaultPreferenceDirectory(appFilesDir), "language.properties")

    private fun defaultPreferenceDirectory(appFilesDir: String): String =
        joinPath(appFilesDir, "gem", "preferences")

    private fun joinPath(
        first: String,
        vararg more: String,
    ): String {
        val separator = if ('\\' in first) "\\" else "/"
        return (listOf(first.trimEnd('/', '\\')) + more.map { it.trim('/', '\\') })
            .joinToString(separator)
    }
}
