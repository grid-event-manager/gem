package org.gem.credential.vault

import java.nio.file.Files
import java.nio.file.Path
import java.security.SecureRandom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.LoginCredentialMaterial
import org.gem.core.domain.SavedAccountProfile
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.domain.SecondLifeLoginUri
import org.gem.core.domain.SharedSecret
import org.gem.core.ports.AccountProfileIdSource
import org.gem.core.ports.AccountProfileStoreDeleteResult
import org.gem.core.ports.AccountProfileStoreListResult
import org.gem.core.ports.AccountProfileStoreSaveResult
import org.gem.core.ports.AccountProfileStoreUpdateResult
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.CredentialVaultDeleteResult
import org.gem.core.ports.CredentialVaultResolveResult
import org.gem.core.ports.CredentialVaultSaveResult
import org.gem.core.ports.CredentialVaultUpdateResult
import org.gem.core.services.CredentialService
import org.gem.core.services.CredentialServiceAddResult
import org.gem.core.services.CredentialServiceDeleteResult
import org.gem.core.services.CredentialServiceUpdatePasswordResult

class EncryptedGemVaultTest {
    @Test
    fun `credential service uses encrypted vault and profile store end to end`() = withTempDirectory { dir ->
        val vaultPath = dir.resolve("vault.bin")
        val ready = openReady(vaultPath, accountProfileIdSource = FixedProfileIdSource("one"))
        val service = CredentialService(
            accountProfileStore = ready.accountProfileStore,
            credentialVault = ready.credentialVault,
            accountProfileIdSource = ready.accountProfileIdSource,
        )

        val added = assertIs<CredentialServiceAddResult.Saved>(
            service.addLogin("VenueHost", "venue-password", startLocation = "uri:London City&76&174&23"),
        )
        val listed = assertIs<AccountProfileStoreListResult.Listed>(service.listProfiles())
        assertEquals(listOf(added.profile), listed.profiles)
        assertVaultFileDoesNotContain(vaultPath, "venue-password")
        assertVaultFileDoesNotContain(vaultPath, "venuehost resident")

        assertEquals(
            CredentialServiceUpdatePasswordResult.Updated,
            service.updatePassword(added.profile.profileId, "changed-password"),
        )
        val updated = assertIs<CredentialVaultResolveResult.Resolved>(
            ready.credentialVault.resolve(added.profile.credentialHandle),
        )
        assertEquals("changed-password", updated.material.sharedSecret.revealForLogin())
        assertVaultFileDoesNotContain(vaultPath, "changed-password")

        assertIs<CredentialServiceDeleteResult.Deleted>(service.deleteProfiles(setOf(added.profile.profileId)))
        val afterDelete = assertIs<AccountProfileStoreListResult.Listed>(service.listProfiles())
        assertEquals(emptyList(), afterDelete.profiles)
        assertIs<CredentialVaultDeleteResult.Missing>(ready.credentialVault.delete(added.profile.credentialHandle))
        val reopened = openReady(vaultPath, accountProfileIdSource = FixedProfileIdSource("two"))
        assertEquals(emptyList(), assertIs<AccountProfileStoreListResult.Listed>(reopened.accountProfileStore.list()).profiles)
    }

    @Test
    fun `vault updates deletes and resolves credential material directly`() = withTempDirectory { dir ->
        val ready = openReady(dir.resolve("vault.bin"))
        val saved = assertIs<CredentialVaultSaveResult.Saved>(ready.credentialVault.save(material("venue-password")))

        val resolved = assertIs<CredentialVaultResolveResult.Resolved>(ready.credentialVault.resolve(saved.credentialHandle))
        assertEquals("venue-password", resolved.material.sharedSecret.revealForLogin())

        val updated = assertIs<CredentialVaultUpdateResult.Updated>(
            ready.credentialVault.update(saved.credentialHandle, material("changed-password")),
        )
        assertEquals(saved.credentialHandle, updated.credentialHandle)

        assertIs<CredentialVaultDeleteResult.Deleted>(ready.credentialVault.delete(saved.credentialHandle))
        assertIs<CredentialVaultResolveResult.Missing>(ready.credentialVault.resolve(saved.credentialHandle))
    }

    @Test
    fun `profile store saves updates lists and deletes profiles inside the vault`() = withTempDirectory { dir ->
        val ready = openReady(dir.resolve("vault.bin"))
        val saved = assertIs<CredentialVaultSaveResult.Saved>(ready.credentialVault.save(material("venue-password")))
        val profile = profile(saved.credentialHandle, label = "venuehost resident")

        assertEquals(AccountProfileStoreSaveResult.Saved(profile), ready.accountProfileStore.save(profile))
        assertEquals(listOf(profile), assertIs<AccountProfileStoreListResult.Listed>(ready.accountProfileStore.list()).profiles)

        val updated = profile.copy(label = "venuehost resident updated")
        assertEquals(AccountProfileStoreUpdateResult.Updated(updated), ready.accountProfileStore.update(updated))
        assertEquals(listOf(updated), assertIs<AccountProfileStoreListResult.Listed>(ready.accountProfileStore.list()).profiles)

        assertIs<CredentialVaultDeleteResult.Deleted>(ready.credentialVault.delete(saved.credentialHandle))
        assertIs<CredentialVaultResolveResult.Missing>(ready.credentialVault.resolve(saved.credentialHandle))
        assertEquals(AccountProfileStoreDeleteResult.Deleted(profile.profileId), ready.accountProfileStore.delete(profile.profileId))
        assertEquals(emptyList(), assertIs<AccountProfileStoreListResult.Listed>(ready.accountProfileStore.list()).profiles)
    }

    @Test
    fun `credential handle collision exhausts after three retries`() = withTempDirectory { dir ->
        val ready = openReady(
            vaultPath = dir.resolve("vault.bin"),
            credentialHandleRandom = FillingSecureRandom(1),
        )
        assertIs<CredentialVaultSaveResult.Saved>(ready.credentialVault.save(material("venue-password")))

        val collision = assertIs<CredentialVaultSaveResult.StorageFailed>(
            ready.credentialVault.save(material("other-password")),
        )

        assertEquals("handle_collision_exhausted", collision.message)
    }

    @Test
    fun `corrupt vault file opens as corrupt vault`() = withTempDirectory { dir ->
        val vaultPath = dir.resolve("vault.bin")
        Files.write(vaultPath, byteArrayOf(1, 2, 3))

        val result = VaultAccessService(FixedVaultKeySource(), GemVaultFileStore(vaultPath)).open()

        assertIs<VaultAccessOpenResult.CorruptVault>(result)
    }

    private fun openReady(
        vaultPath: Path,
        credentialHandleRandom: SecureRandom = CountingSecureRandom(),
        accountProfileIdSource: AccountProfileIdSource = FixedProfileIdSource("profile"),
    ): VaultAccessOpenResult.Ready =
        assertIs<VaultAccessOpenResult.Ready>(
            VaultAccessService(
                keySource = FixedVaultKeySource(),
                fileStore = GemVaultFileStore(vaultPath),
                accountProfileIdSource = accountProfileIdSource,
                credentialHandleRandom = credentialHandleRandom,
            ).open(),
        )

    private fun material(password: String): LoginCredentialMaterial =
        LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = loginName("venuehost"),
            sharedSecret = assertNotNull(SharedSecret.fromPlainText(password)),
            startLocation = "uri:London City&76&174&23",
        )

    private fun profile(
        credentialHandle: CredentialHandle,
        label: String,
    ): SavedAccountProfile =
        SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:one"),
            loginName = loginName("venuehost"),
            label = label,
            credentialHandle = credentialHandle,
            startLocation = "uri:London City&76&174&23",
        )

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName

    private fun assertVaultFileDoesNotContain(
        vaultPath: Path,
        value: String,
    ) {
        assertFalse(Files.readAllBytes(vaultPath).containsSubsequence(value.encodeToByteArray()))
    }

    private fun withTempDirectory(assertion: (Path) -> Unit) {
        val dir = Files.createTempDirectory("gem-encrypted-vault-test")
        try {
            assertion(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}

private class FixedVaultKeySource(
    private val material: JcaVaultKeyMaterial = requireNotNull(JcaVaultKeyMaterial.fromRawKeyBytes(ByteArray(32) { 42 })),
) : VaultKeySource {
    override fun getOrCreateKey(): VaultKeySourceResult =
        VaultKeySourceResult.Loaded(material)

    override fun deleteKey(): VaultKeySourceDeleteResult =
        VaultKeySourceDeleteResult.Deleted
}

private class FixedProfileIdSource(
    suffix: String,
) : AccountProfileIdSource {
    private val profileId = AccountProfileId("profile:v1:$suffix")

    override fun nextProfileId(): AccountProfileId = profileId
}

private class CountingSecureRandom : SecureRandom() {
    private var next: Byte = 0

    override fun nextBytes(bytes: ByteArray) {
        bytes.indices.forEach { index ->
            bytes[index] = next
            next = (next + 1).toByte()
        }
    }
}

private fun ByteArray.containsSubsequence(needle: ByteArray): Boolean {
    for (index in 0..(size - needle.size)) {
        var found = true
        for (needleIndex in needle.indices) {
            if (this[index + needleIndex] != needle[needleIndex]) {
                found = false
                break
            }
        }
        if (found) {
            return true
        }
    }
    return false
}
