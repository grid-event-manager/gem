package org.hostess.apps.android

import java.io.File
import java.nio.file.Path
import org.hostess.core.preferences.LastLoginProfilePreferenceService
import org.hostess.core.theme.ThemePreferenceService
import org.hostess.preferences.AndroidHostessPreferencePaths
import org.hostess.preferences.FileLastLoginProfilePreferenceStore
import org.hostess.preferences.FileThemePreferenceStore

object HostessAndroidPreferenceComposition {
    fun open(appFilesDir: File): ThemePreferenceService {
        val preferenceFile = File(
            AndroidHostessPreferencePaths.defaultPreferenceFile(appFilesDir.path),
        )
        return ThemePreferenceService(FileThemePreferenceStore(preferenceFile.toPath()))
    }

    fun openLastLoginProfile(appFilesDir: File): LastLoginProfilePreferenceService {
        val preferenceFile = File(
            AndroidHostessPreferencePaths.defaultLastLoginProfileFile(appFilesDir.path),
        )
        return LastLoginProfilePreferenceService(FileLastLoginProfilePreferenceStore(preferenceFile.toPath()))
    }

    fun inventorySnapshotCacheDirectory(appFilesDir: File): Path =
        File(appFilesDir, "Hostess/inventory-snapshots").toPath()
}
