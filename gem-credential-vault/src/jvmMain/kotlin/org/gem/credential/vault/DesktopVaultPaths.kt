package org.gem.credential.vault

object DesktopVaultPaths {
    fun defaultVaultDirectory(
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
        return joinPath(base, "Hostess", "vault")
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
