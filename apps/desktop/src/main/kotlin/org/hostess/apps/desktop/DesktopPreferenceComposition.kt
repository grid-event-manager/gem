package org.hostess.apps.desktop

import java.nio.file.Path
import org.hostess.core.preferences.LastLoginProfilePreferenceService
import org.hostess.core.theme.ThemePreferenceService
import org.hostess.preferences.DesktopHostessPreferencePaths
import org.hostess.preferences.FileLastLoginProfilePreferenceStore
import org.hostess.preferences.FileThemePreferenceStore

object DesktopPreferenceComposition {
    fun open(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): ThemePreferenceService {
        val preferenceFile = Path.of(
            DesktopHostessPreferencePaths.defaultPreferenceFile(osName, env, userHome),
        )
        return ThemePreferenceService(FileThemePreferenceStore(preferenceFile))
    }

    fun openLastLoginProfile(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): LastLoginProfilePreferenceService {
        val preferenceFile = Path.of(
            DesktopHostessPreferencePaths.defaultLastLoginProfileFile(osName, env, userHome),
        )
        return LastLoginProfilePreferenceService(FileLastLoginProfilePreferenceStore(preferenceFile))
    }

    fun inventorySnapshotCacheDirectory(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): Path =
        Path.of(DesktopHostessPreferencePaths.defaultPreferenceFile(osName, env, userHome))
            .parent
            .resolve("inventory-snapshots")
}
