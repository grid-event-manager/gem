package org.gem.apps.desktop

import java.nio.file.Path
import org.gem.core.appearance.AppearanceProfileService
import org.gem.core.language.LanguagePreferenceService
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.theme.ThemePreferenceService
import org.gem.preferences.DesktopGemPreferencePaths
import org.gem.preferences.FileAppearanceProfileStore
import org.gem.preferences.FileLanguagePreferenceStore
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

    fun openAppearanceProfiles(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): AppearanceProfileService {
        val preferenceFile = Path.of(
            DesktopGemPreferencePaths.defaultAppearanceProfileFile(osName, env, userHome),
        )
        return AppearanceProfileService(FileAppearanceProfileStore(preferenceFile))
    }

    fun openLanguagePreference(
        osName: String = System.getProperty("os.name").orEmpty(),
        env: Map<String, String> = System.getenv(),
        userHome: String = System.getProperty("user.home").orEmpty(),
    ): LanguagePreferenceService {
        val preferenceFile = Path.of(
            DesktopGemPreferencePaths.defaultLanguagePreferenceFile(osName, env, userHome),
        )
        return LanguagePreferenceService(FileLanguagePreferenceStore(preferenceFile))
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
