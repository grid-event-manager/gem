package org.hostess.apps.android

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SavedAccountProfile
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.ports.CredentialHandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HostessAndroidCompositionRootTest {
    @Test
    fun `creates shared UI runtime through Android app-shell root`() {
        val appFilesDir = Files.createTempDirectory("hostess-android-composition-root-test")
        try {
            val runtime = HostessAndroidCompositionRoot.create(appFilesDir.toFile())

            assertNotNull(runtime.credentialRuntimeState)
            assertNotNull(runtime.sessionService)
            assertNotNull(runtime.avatarReadinessService)
            assertNotNull(runtime.groupDirectoryService)
            assertNotNull(runtime.inventoryDirectoryService)
            assertNotNull(runtime.noticeDispatchService)
            val compliance = runtime.loginComplianceProvider.requestFor(fakeProfile())
            assertTrue(compliance.proofAccountAttested)
            assertTrue(compliance.automatedUse)
            assertEquals("Android proof account", compliance.proofAccountLabel)
        } finally {
            appFilesDir.deleteRecursively()
        }
    }

    private fun fakeProfile(): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:android-proof"),
            loginName = loginName(),
            label = "Android proof account",
            credentialHandle = CredentialHandle("hostess-vault:v1:android-proof"),
            startLocation = null,
        )

    private fun loginName(): SecondLifeLoginName =
        when (val result = SecondLifeLoginName.fromUserInput("android proof")) {
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
