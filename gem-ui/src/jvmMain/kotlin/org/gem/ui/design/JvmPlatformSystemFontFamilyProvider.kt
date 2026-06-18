package org.gem.ui.design

import org.gem.core.appearance.AppearanceFontFamily

class JvmPlatformSystemFontFamilyProvider(
    private val osName: String = System.getProperty("os.name").orEmpty(),
) : PlatformSystemFontFamilyProvider {
    override fun defaultFamily(availableFamilies: List<AppearanceFontFamily>): AppearanceFontFamily =
        PlatformSystemFontFamilySelection.select(
            availableFamilies = availableFamilies,
            candidates = candidatesFor(osName),
        )

    private fun candidatesFor(value: String): List<String> {
        val normalized = value.lowercase()
        return when {
            normalized.contains("mac") || normalized.contains("darwin") -> MACOS_CHAIN
            normalized.contains("windows") || normalized.startsWith("win") -> WINDOWS_CHAIN
            else -> LINUX_UNIX_CHAIN
        }
    }

    private companion object {
        val LINUX_UNIX_CHAIN = listOf("Noto Sans", "DejaVu Sans", "Arial", "sans-serif")
        val WINDOWS_CHAIN = listOf("Segoe UI", "Arial", "sans-serif")
        val MACOS_CHAIN = listOf("Helvetica Neue", "Helvetica", "sans-serif")
    }
}
