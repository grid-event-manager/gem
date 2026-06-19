package org.gem.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileSaveResult
import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.ports.CredentialHandle
import org.gem.core.preferences.LastLoginProfilePreferenceSaveResult
import org.gem.core.services.GemCredentialRuntimeReady
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.preferences.DesktopGemPreferencePaths
import org.gem.ui.design.JvmPlatformSystemFontFamilyProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemDesktopCompositionRootTest {
    @Test
    fun `creates shared UI runtime through desktop app-shell root`() {
        val tempDataHome = Files.createTempDirectory("gem-desktop-composition-root-test")
        try {
            val runtime = GemDesktopCompositionRoot.create(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
                userHome = tempDataHome.resolve("home").toString(),
            )

            assertIs<GemCredentialRuntimeReady>(runtime.credentialRuntimeState)
            assertNotNull(runtime.credentialServiceOrNull())
            assertNotNull(runtime.sessionService)
            assertNotNull(runtime.avatarReadinessService)
            assertNotNull(runtime.groupDirectoryService)
            assertNotNull(runtime.inventoryDirectoryService)
            assertNotNull(runtime.noticeDispatchService)
            assertTrue(runtime.platformFontCatalogue.availableFamilies().isNotEmpty())
            assertNotNull(runtime.platformFontFamilyResolver.resolve(AppearanceFontFamily("sans-serif")))
            assertIs<JvmPlatformSystemFontFamilyProvider>(runtime.platformSystemFontFamilyProvider)
            val initialThemePreference = runtime.themePreferenceService.loadPreference()
            assertEquals(ThemePreference.SYSTEM, initialThemePreference.preference)
            assertNull(initialThemePreference.warning)
            assertEquals(ThemePreferenceSaveResult.Saved, runtime.themePreferenceService.savePreference(ThemePreference.DARK))
            assertTrue(Files.exists(Path.of(preferenceFile(tempDataHome))))
            assertEquals(
                LastLoginProfilePreferenceSaveResult.Saved,
                runtime.lastLoginProfilePreferenceService.saveProfileId(AccountProfileId("profile:v1:last")),
            )
            assertTrue(Files.exists(Path.of(lastLoginProfileFile(tempDataHome))))
            val appearanceSave = runtime.appearanceProfileService.saveProfile(
                name = AppearanceProfileName("Desktop Proof"),
                mode = AppearanceMode.LIGHT,
                draft = stockDraft(AppearanceMode.LIGHT),
            )
            assertTrue(appearanceSave is AppearanceProfileSaveResult.Saved)
            assertTrue(Files.exists(Path.of(appearanceProfileFile(tempDataHome))))
            val compliance = runtime.loginComplianceProvider.requestFor(fakeProfile())
            assertTrue(compliance.proofAccountAttested)
            assertTrue(compliance.automatedUse)
            assertEquals("Desktop proof account", compliance.proofAccountLabel)
        } finally {
            DesktopTestDirectoryCleaner.deleteRecursively(tempDataHome)
        }
    }

    @Test
    fun `migrates legacy desktop storage before opening app stores`() {
        val tempDataHome = Files.createTempDirectory("gem-desktop-composition-migration-test")
        val legacyVault = tempDataHome.resolve("Hostess/vault")
        val legacyPreferences = tempDataHome.resolve("Hostess/preferences")
        try {
            Files.createDirectories(legacyVault)
            Files.createDirectories(legacyPreferences.resolve("inventory-snapshots"))
            Files.writeString(legacyPreferences.resolve("ui.properties"), "themePreference=LIGHT\n")
            Files.writeString(legacyPreferences.resolve("last-login-profile.txt"), "profile:v1:legacy")
            Files.writeString(legacyPreferences.resolve("inventory-snapshots/cache.json"), "{}")

            val runtime = GemDesktopCompositionRoot.create(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
                userHome = tempDataHome.resolve("home").toString(),
            )

            assertFalse(Files.exists(legacyVault))
            assertFalse(Files.exists(legacyPreferences))
            assertTrue(Files.exists(tempDataHome.resolve("gem/vault/vault.bin")))
            assertEquals(ThemePreference.LIGHT, runtime.themePreferenceService.loadPreference().preference)
            assertTrue(Files.exists(tempDataHome.resolve("gem/preferences/last-login-profile.txt")))
            assertTrue(Files.exists(tempDataHome.resolve("gem/preferences/inventory-snapshots/cache.json")))
        } finally {
            DesktopTestDirectoryCleaner.deleteRecursively(tempDataHome)
        }
    }

    private fun fakeProfile(): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:desktop-proof"),
            loginName = loginName(),
            label = "Desktop proof account",
            credentialHandle = CredentialHandle("gem-vault:v1:desktop-proof"),
            startLocation = null,
        )

    private fun preferenceFile(tempDataHome: Path): String =
        DesktopGemPreferencePaths.defaultPreferenceFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
            userHome = tempDataHome.resolve("home").toString(),
        )

    private fun lastLoginProfileFile(tempDataHome: Path): String =
        DesktopGemPreferencePaths.defaultLastLoginProfileFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
            userHome = tempDataHome.resolve("home").toString(),
        )

    private fun appearanceProfileFile(tempDataHome: Path): String =
        DesktopGemPreferencePaths.defaultAppearanceProfileFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
            userHome = tempDataHome.resolve("home").toString(),
        )

    private fun loginName(): SecondLifeLoginName =
        when (val result = SecondLifeLoginName.fromUserInput("desktop proof")) {
            is SecondLifeLoginNameResult.Valid -> result.loginName
            is SecondLifeLoginNameResult.Invalid -> error("invalid test login name")
        }

    private fun stockDraft(mode: AppearanceMode): AppearanceDraft =
        AppearanceDraft.fromProfile(AppearanceProfileCatalogue.stockProfiles().first { it.mode == mode })

}
