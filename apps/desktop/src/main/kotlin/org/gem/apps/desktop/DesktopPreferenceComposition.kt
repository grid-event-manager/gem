package org.gem.apps.desktop

import java.nio.file.Path
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.theme.ThemePreferenceService
import org.gem.preferences.DesktopGemPreferencePaths
import org.gem.preferences.FileLastLoginProfilePreferenceStore
import org.gem.preferences.FileThemePreferenceStore

object DesktopPreferenceComposition {
    fun open(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): ThemePreferenceService {
        val preferenceFile = Path.of(
            DesktopGemPreferencePaths.defaultPreferenceFile(osName, env, userHome),
        )
        return ThemePreferenceService(FileThemePreferenceStore(preferenceFile))
    }

    fun openLastLoginProfile(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): LastLoginProfilePreferenceService {
        val preferenceFile = Path.of(
            DesktopGemPreferencePaths.defaultLastLoginProfileFile(osName, env, userHome),
        )
        return LastLoginProfilePreferenceService(FileLastLoginProfilePreferenceStore(preferenceFile))
    }

    fun inventorySnapshotCacheDirectory(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): Path =
        Path.of(DesktopGemPreferencePaths.defaultPreferenceFile(osName, env, userHome))
            .parent
            .resolve("inventory-snapshots")
}
