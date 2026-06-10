package org.hostess.apps.android

import java.io.File
import org.hostess.core.theme.ThemePreferenceService
import org.hostess.preferences.AndroidHostessPreferencePaths
import org.hostess.preferences.FileThemePreferenceStore

object HostessAndroidPreferenceComposition {
    fun open(appFilesDir: File): ThemePreferenceService {
        val preferenceFile = File(
            AndroidHostessPreferencePaths.defaultPreferenceFile(appFilesDir.path),
        )
        return ThemePreferenceService(FileThemePreferenceStore(preferenceFile.toPath()))
    }
}
