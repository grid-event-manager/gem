package org.gem.apps.android

import java.io.File
import java.nio.file.Path
import org.gem.core.preferences.LastLoginProfilePreferenceService
import org.gem.core.theme.ThemePreferenceService
import org.gem.preferences.AndroidGemPreferencePaths
import org.gem.preferences.FileLastLoginProfilePreferenceStore
import org.gem.preferences.FileThemePreferenceStore

object GemAndroidPreferenceComposition {
    fun open(appFilesDir: File): ThemePreferenceService {
        val preferenceFile = File(
            AndroidGemPreferencePaths.defaultPreferenceFile(appFilesDir.path),
        )
        return ThemePreferenceService(FileThemePreferenceStore(preferenceFile.toPath()))
    }

    fun openLastLoginProfile(appFilesDir: File): LastLoginProfilePreferenceService {
        val preferenceFile = File(
            AndroidGemPreferencePaths.defaultLastLoginProfileFile(appFilesDir.path),
        )
        return LastLoginProfilePreferenceService(FileLastLoginProfilePreferenceStore(preferenceFile.toPath()))
    }

    fun inventorySnapshotCacheDirectory(appFilesDir: File): Path =
        File(appFilesDir, "gem/inventory-snapshots").toPath()
}
