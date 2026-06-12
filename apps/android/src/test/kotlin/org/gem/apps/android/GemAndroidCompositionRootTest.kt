package org.gem.apps.android

import java.nio.file.Files
import java.nio.file.Path
import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.ports.CredentialHandle
import org.gem.core.preferences.LastLoginProfilePreferenceSaveResult
import org.gem.core.theme.ThemePreference
import org.gem.core.theme.ThemePreferenceSaveResult
import org.gem.preferences.AndroidGemPreferencePaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GemAndroidCompositionRootTest {
    @Test
    fun `creates shared UI runtime through Android app-shell root`() {
        val appFilesDir = Files.createTempDirectory("gem-android-composition-root-test")
        try {
            val runtime = GemAndroidCompositionRoot.create(appFilesDir.toFile())

            assertNotNull(runtime.credentialRuntimeState)
            assertNotNull(runtime.sessionService)
            assertNotNull(runtime.avatarReadinessService)
            assertNotNull(runtime.groupDirectoryService)
            assertNotNull(runtime.inventoryDirectoryService)
            assertNotNull(runtime.noticeDispatchService)
            assertEquals(ThemePreferenceSaveResult.Saved, runtime.themePreferenceService.savePreference(ThemePreference.DARK))
            assertTrue(Files.exists(Path.of(preferenceFile(appFilesDir))))
            assertEquals(
                LastLoginProfilePreferenceSaveResult.Saved,
                runtime.lastLoginProfilePreferenceService.saveProfileId(AccountProfileId("profile:v1:last")),
            )
            assertTrue(Files.exists(Path.of(lastLoginProfileFile(appFilesDir))))
            val compliance = runtime.loginComplianceProvider.requestFor(fakeProfile())
            assertTrue(compliance.proofAccountAttested)
            assertTrue(compliance.automatedUse)
            assertEquals("Android proof account", compliance.proofAccountLabel)
        } finally {
            GemAndroidTestDirectoryCleaner.deleteRecursively(appFilesDir)
        }
    }

    private fun fakeProfile(): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:android-proof"),
            loginName = loginName(),
            label = "Android proof account",
            credentialHandle = CredentialHandle("gem-vault:v1:android-proof"),
            startLocation = null,
        )

    private fun preferenceFile(appFilesDir: Path): String =
        AndroidGemPreferencePaths.defaultPreferenceFile(appFilesDir.toString())

    private fun lastLoginProfileFile(appFilesDir: Path): String =
        AndroidGemPreferencePaths.defaultLastLoginProfileFile(appFilesDir.toString())

    private fun loginName(): SecondLifeLoginName =
        when (val result = SecondLifeLoginName.fromUserInput("android proof")) {
            is SecondLifeLoginNameResult.Valid -> result.loginName
            is SecondLifeLoginNameResult.Invalid -> error("invalid test login name")
        }

}
