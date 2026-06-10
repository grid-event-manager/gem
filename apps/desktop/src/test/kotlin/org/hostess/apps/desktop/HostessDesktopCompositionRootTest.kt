package org.hostess.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.theme.ThemePreference
import org.hostess.core.theme.ThemePreferenceSaveResult
import org.hostess.preferences.DesktopHostessPreferencePaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HostessDesktopCompositionRootTest {
    @Test
    fun `creates shared UI runtime through desktop app-shell root`() {
        val tempDataHome = Files.createTempDirectory("hostess-desktop-composition-root-test")
        try {
            val runtime = HostessDesktopCompositionRoot.create(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
                userHome = tempDataHome.resolve("home").toString(),
            )

            assertIs<HostessCredentialRuntimeReady>(runtime.credentialRuntimeState)
            assertNotNull(runtime.credentialServiceOrNull())
            assertNotNull(runtime.sessionService)
            assertNotNull(runtime.avatarReadinessService)
            assertNotNull(runtime.groupDirectoryService)
            assertNotNull(runtime.inventoryDirectoryService)
            assertNotNull(runtime.noticeDispatchService)
            val initialThemePreference = runtime.themePreferenceService.loadPreference()
            assertEquals(ThemePreference.SYSTEM, initialThemePreference.preference)
            assertNull(initialThemePreference.warning)
            assertEquals(ThemePreferenceSaveResult.Saved, runtime.themePreferenceService.savePreference(ThemePreference.DARK))
            assertTrue(Files.exists(Path.of(preferenceFile(tempDataHome))))
            val compliance = runtime.loginComplianceProvider.requestFor(fakeProfile())
            assertTrue(compliance.proofAccountAttested)
            assertTrue(compliance.automatedUse)
            assertEquals("Desktop proof account", compliance.proofAccountLabel)
        } finally {
            DesktopTestDirectoryCleaner.deleteRecursively(tempDataHome)
        }
    }

    private fun fakeProfile(): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:desktop-proof"),
            loginName = loginName(),
            label = "Desktop proof account",
            credentialHandle = CredentialHandle("hostess-vault:v1:desktop-proof"),
            startLocation = null,
        )

    private fun preferenceFile(tempDataHome: Path): String =
        DesktopHostessPreferencePaths.defaultPreferenceFile(
            osName = "Linux",
            env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
            userHome = tempDataHome.resolve("home").toString(),
        )

    private fun loginName(): SecondLifeLoginName =
        when (val result = SecondLifeLoginName.fromUserInput("desktop proof")) {
            is SecondLifeLoginNameResult.Valid -> result.loginName
            is SecondLifeLoginNameResult.Invalid -> error("invalid test login name")
        }

}
