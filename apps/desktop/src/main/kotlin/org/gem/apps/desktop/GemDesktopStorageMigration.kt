package org.gem.apps.desktop

import org.gem.credential.vault.LegacyDesktopVaultMigration
import org.gem.credential.vault.LegacyDesktopVaultMigrationResult
import org.gem.preferences.LegacyDesktopPreferenceMigration
import org.gem.preferences.LegacyDesktopPreferenceMigrationResult

object GemDesktopStorageMigration {
    fun run(): GemDesktopStorageMigrationResult =
        run(
            osName = System.getProperty("os.name").orEmpty(),
            env = System.getenv(),
            userHome = System.getProperty("user.home").orEmpty(),
        )

    fun run(
        osName: String,
        env: Map<String, String>,
        userHome: String,
    ): GemDesktopStorageMigrationResult =
        GemDesktopStorageMigrationResult(
            vault = LegacyDesktopVaultMigration.run(osName, env, userHome),
            preferences = LegacyDesktopPreferenceMigration.run(osName, env, userHome),
        )
}

data class GemDesktopStorageMigrationResult(
    val vault: LegacyDesktopVaultMigrationResult,
    val preferences: LegacyDesktopPreferenceMigrationResult,
)
