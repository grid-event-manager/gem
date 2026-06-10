package org.hostess.apps.desktop

import java.nio.file.Path
import org.hostess.core.theme.ThemePreferenceService
import org.hostess.preferences.DesktopHostessPreferencePaths
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
}
