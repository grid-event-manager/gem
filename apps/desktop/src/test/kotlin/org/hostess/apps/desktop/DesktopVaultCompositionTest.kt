package org.hostess.apps.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import org.hostess.core.services.HostessCredentialRuntimeReady
import org.hostess.core.services.HostessCredentialRuntimeResetReason
import org.hostess.core.services.HostessCredentialRuntimeResetRequired
import org.hostess.core.services.HostessCredentialRuntimeUnavailable
import org.hostess.core.services.HostessCredentialRuntimeUnavailableReason
import org.hostess.credential.vault.LocalUserFileVaultKeySource
import org.hostess.credential.vault.VaultAccessOpenResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopVaultCompositionTest {
    @Test
    fun `creates desktop credential runtime state from temp app data`() {
        val tempDataHome = Files.createTempDirectory("hostess-desktop-vault-test")
        try {
            val state = DesktopVaultComposition.create(
                osName = "Linux",
                env = mapOf("XDG_DATA_HOME" to tempDataHome.toString()),
                userHome = tempDataHome.resolve("home").toString(),
            )

            assertIs<HostessCredentialRuntimeReady>(state)
            assertTrue(Files.exists(tempDataHome.resolve("Hostess/vault/vault.bin")))
            assertTrue(Files.exists(tempDataHome.resolve("Hostess/vault/${LocalUserFileVaultKeySource.KEY_FILE_NAME}")))
        } finally {
            tempDataHome.deleteRecursively()
        }
    }

    @Test
    fun `maps unavailable and reset-required vault failures exactly`() {
        assertUnavailable(
            VaultAccessOpenResult.KeySourceFailed("key_source_failed"),
            HostessCredentialRuntimeUnavailableReason.KEY_SOURCE_FAILED,
            "key_source_failed",
        )
        assertUnavailable(
            VaultAccessOpenResult.StorageFailed("storage_failed"),
            HostessCredentialRuntimeUnavailableReason.STORAGE_FAILED,
            "storage_failed",
        )
        assertResetRequired(
            VaultAccessOpenResult.CryptoFailed("crypto_failed"),
            HostessCredentialRuntimeResetReason.CRYPTO_FAILED,
            "crypto_failed",
        )
        assertResetRequired(
            VaultAccessOpenResult.CorruptVault("corrupt"),
            HostessCredentialRuntimeResetReason.CORRUPT_VAULT,
            "corrupt",
        )
    }

    private fun assertUnavailable(
        openResult: VaultAccessOpenResult,
        reason: HostessCredentialRuntimeUnavailableReason,
        message: String,
    ) {
        val state = assertIs<HostessCredentialRuntimeUnavailable>(
            DesktopVaultComposition.mapOpenResult(openResult),
        )
        assertEquals(reason, state.reason)
        assertEquals(message, state.message)
    }

    private fun assertResetRequired(
        openResult: VaultAccessOpenResult,
        reason: HostessCredentialRuntimeResetReason,
        message: String,
    ) {
        val state = assertIs<HostessCredentialRuntimeResetRequired>(
            DesktopVaultComposition.mapOpenResult(openResult),
        )
        assertEquals(reason, state.reason)
        assertEquals(message, state.message)
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
