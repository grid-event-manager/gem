package org.hostess.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.services.HostessCredentialRuntimeReady
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
            val compliance = runtime.loginComplianceProvider.requestFor(fakeProfile())
            assertTrue(compliance.proofAccountAttested)
            assertTrue(compliance.automatedUse)
            assertEquals("Desktop proof account", compliance.proofAccountLabel)
        } finally {
            tempDataHome.deleteRecursively()
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

    private fun loginName(): SecondLifeLoginName =
        when (val result = SecondLifeLoginName.fromUserInput("desktop proof")) {
            is SecondLifeLoginNameResult.Valid -> result.loginName
            is SecondLifeLoginNameResult.Invalid -> error("invalid test login name")
        }

    private fun Path.deleteRecursively() {
        if (!Files.exists(this)) {
            return
        }
        Files.walk(this).use { paths ->
            paths.sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }
}
