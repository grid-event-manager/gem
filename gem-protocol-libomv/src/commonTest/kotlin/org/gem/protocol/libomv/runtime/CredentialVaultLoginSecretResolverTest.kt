package org.gem.protocol.libomv.runtime

import org.gem.core.domain.LoginCredentialMaterial
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.domain.SecondLifeLoginUri
import org.gem.core.domain.SharedSecret
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.CredentialVault
import org.gem.core.ports.CredentialVaultDeleteResult
import org.gem.core.ports.CredentialVaultResolveResult
import org.gem.core.ports.CredentialVaultSaveResult
import org.gem.core.ports.CredentialVaultUpdateResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class CredentialVaultLoginSecretResolverTest {
    @Test
    fun `maps vault material to existing login secret contract`() {
        val vault = FakeCredentialVault(
            result = CredentialVaultResolveResult.Resolved(
                material(
                    loginName = "venuehost",
                    sharedSecret = "venue-password",
                    startLocation = "uri:London City&76&174&23",
                ),
            ),
        )

        val secret = CredentialVaultLoginSecretResolver(vault).resolve(VAULT_HANDLE)

        requireNotNull(secret)
        assertEquals(SecondLifeLoginUri.SECOND_LIFE_DEFAULT.value, secret.loginUri)
        assertEquals("venuehost", secret.firstName)
        assertEquals("resident", secret.lastName)
        assertEquals("venue-password", secret.sharedSecret)
        assertEquals("uri:London City&76&174&23", secret.startLocation)
        assertEquals(listOf(VAULT_HANDLE), vault.resolvedHandles)
    }

    @Test
    fun `uses protocol default start location when material has none`() {
        val secret = CredentialVaultLoginSecretResolver(
            FakeCredentialVault(CredentialVaultResolveResult.Resolved(material(startLocation = null))),
        ).resolve(VAULT_HANDLE)

        assertEquals("last", secret?.startLocation)
    }

    @Test
    fun `ignores non vault handles without touching vault`() {
        val vault = FakeCredentialVault(CredentialVaultResolveResult.Resolved(material()))

        assertNull(CredentialVaultLoginSecretResolver(vault).resolve(CredentialHandle("HOSTESS_SL_SECRET")))

        assertEquals(emptyList(), vault.resolvedHandles)
    }

    @Test
    fun `maps every vault resolve failure to null`() {
        val failures = listOf(
            CredentialVaultResolveResult.Missing(VAULT_HANDLE, "[redacted]"),
            CredentialVaultResolveResult.KeySourceFailed("[redacted]"),
            CredentialVaultResolveResult.CryptoFailed("[redacted]"),
            CredentialVaultResolveResult.CorruptVault("[redacted]"),
            CredentialVaultResolveResult.StorageFailed("[redacted]"),
        )

        failures.forEach { failure ->
            assertNull(CredentialVaultLoginSecretResolver(FakeCredentialVault(failure)).resolve(VAULT_HANDLE))
        }
    }

    @Test
    fun `resolver string and vault failure text do not expose secret`() {
        val vault = FakeCredentialVault(
            result = CredentialVaultResolveResult.Resolved(material(sharedSecret = "venue-password")),
        )

        val resolver = CredentialVaultLoginSecretResolver(vault)
        val secret = requireNotNull(resolver.resolve(VAULT_HANDLE))

        assertEquals("venue-password", secret.sharedSecret)
        assertFalse(resolver.toString().contains("venue-password"), resolver.toString())
        assertFalse(vault.toString().contains("venue-password"), vault.toString())
    }

    private companion object {
        val VAULT_HANDLE = CredentialHandle("gem-vault:v1:test")

        fun material(
            loginName: String = "venuehost resident",
            sharedSecret: String = "venue-password",
            startLocation: String? = "last",
        ): LoginCredentialMaterial = LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = loginName(loginName),
            sharedSecret = requireNotNull(SharedSecret.fromPlainText(sharedSecret)),
            startLocation = startLocation,
        )

        fun loginName(input: String): SecondLifeLoginName =
            assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName
    }
}

private class FakeCredentialVault(
    private val result: CredentialVaultResolveResult,
) : CredentialVault {
    val resolvedHandles = mutableListOf<CredentialHandle>()

    override fun save(material: LoginCredentialMaterial): CredentialVaultSaveResult =
        error("save is not part of resolver proof")

    override fun update(
        credentialHandle: CredentialHandle,
        material: LoginCredentialMaterial,
    ): CredentialVaultUpdateResult =
        error("update is not part of resolver proof")

    override fun delete(credentialHandle: CredentialHandle): CredentialVaultDeleteResult =
        error("delete is not part of resolver proof")

    override fun resolve(credentialHandle: CredentialHandle): CredentialVaultResolveResult {
        resolvedHandles += credentialHandle
        return result
    }
}
