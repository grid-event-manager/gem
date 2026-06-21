package org.gem.preferences

object DesktopGemPreferencePaths {
    fun defaultPreferenceFile(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): String =
        joinPath(defaultPreferenceDirectory(osName, env, userHome), "ui.properties")

    fun defaultLastLoginProfileFile(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): String =
        joinPath(defaultPreferenceDirectory(osName, env, userHome), "last-login-profile.txt")

    fun defaultAppearanceProfileFile(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): String =
        joinPath(defaultPreferenceDirectory(osName, env, userHome), "appearance-profiles.properties")

    fun defaultLanguagePreferenceFile(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): String =
        joinPath(defaultPreferenceDirectory(osName, env, userHome), "language.properties")

    fun defaultPreferenceDirectory(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): String {
        val normalizedOs = osName.lowercase()
        val base = when {
            normalizedOs.startsWith("windows") ->
                env["APPDATA"].orEmpty().ifBlank { joinPath(userHome, "AppData", "Roaming") }
            normalizedOs.startsWith("mac") || normalizedOs.contains("darwin") ->
                joinPath(userHome, "Library", "Application Support")
            else ->
                env["XDG_DATA_HOME"].orEmpty().ifBlank { joinPath(userHome, ".local", "share") }
        }
        return joinPath(base, "gem", "preferences")
    }

    private fun joinPath(
        first: String,
        vararg more: String,
    ): String {
        val separator = if ('\\' in first) "\\" else "/"
        return (listOf(first.trimEnd('/', '\\')) + more.map { it.trim('/', '\\') })
            .joinToString(separator)
    }
}
