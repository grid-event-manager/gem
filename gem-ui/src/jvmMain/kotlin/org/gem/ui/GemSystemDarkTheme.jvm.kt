package org.gem.ui

import java.util.concurrent.TimeUnit

internal actual fun gemPlatformPrefersDarkTheme(): Boolean? =
    JvmSystemDarkThemeDetector.prefersDarkTheme(
        osName = System.getProperty("os.name").orEmpty(),
        commandRunner = ::runSystemThemeCommand,
    )

internal object JvmSystemDarkThemeDetector {
    fun prefersDarkTheme(
        osName: String,
        commandRunner: (List<String>) -> String?,
    ): Boolean? {
        val normalizedOs = osName.lowercase()
        return when {
            normalizedOs.contains("linux") ||
                normalizedOs.contains("nux") ||
                normalizedOs.contains("bsd") -> linuxPrefersDarkTheme(commandRunner)

            normalizedOs.contains("mac") ||
                normalizedOs.contains("darwin") -> macPrefersDarkTheme(commandRunner)

            normalizedOs.contains("win") -> windowsPrefersDarkTheme(commandRunner)
            else -> null
        }
    }

    fun parseLinuxColorScheme(value: String?): Boolean? {
        val normalized = value.cleanThemeValue()
        return when (normalized) {
            "prefer-dark" -> true
            "prefer-light" -> false
            else -> null
        }
    }

    fun parseThemeName(value: String?): Boolean? {
        val normalized = value.cleanThemeValue()
        return when {
            normalized.contains("dark") -> true
            normalized.contains("light") -> false
            else -> null
        }
    }

    fun parseWindowsAppsUseLightTheme(value: String?): Boolean? {
        val normalized = value.orEmpty().lowercase()
        return when {
            "0x0" in normalized ||
                Regex("""\b0\b""").containsMatchIn(normalized) -> true

            "0x1" in normalized ||
                Regex("""\b1\b""").containsMatchIn(normalized) -> false

            else -> null
        }
    }

    private fun linuxPrefersDarkTheme(commandRunner: (List<String>) -> String?): Boolean? {
        parseLinuxColorScheme(
            commandRunner(listOf("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")),
        )?.let { return it }

        parseThemeName(
            commandRunner(listOf("gsettings", "get", "org.gnome.desktop.interface", "gtk-theme")),
        )?.let { return it }

        return null
    }

    private fun macPrefersDarkTheme(commandRunner: (List<String>) -> String?): Boolean? =
        parseThemeName(commandRunner(listOf("defaults", "read", "-g", "AppleInterfaceStyle")))

    private fun windowsPrefersDarkTheme(commandRunner: (List<String>) -> String?): Boolean? =
        parseWindowsAppsUseLightTheme(
            commandRunner(
                listOf(
                    "reg",
                    "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v",
                    "AppsUseLightTheme",
                ),
            ),
        )

    private fun String?.cleanThemeValue(): String =
        orEmpty()
            .trim()
            .trim('\'', '"')
            .lowercase()
}

private fun runSystemThemeCommand(command: List<String>): String? =
    try {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(300, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            null
        } else if (process.exitValue() == 0) {
            process.inputStream.bufferedReader().readText().trim()
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
