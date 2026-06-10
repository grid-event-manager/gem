package org.hostess.credential.vault

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.domain.SecondLifeLoginUri
import org.hostess.core.domain.SharedSecret
import org.hostess.core.ports.CredentialHandle
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class HostessVaultFileCodecTest {
    @Test
    fun `round trips deterministically and sorts records`() {
        val plaintext = HostessVaultPlaintext(
            profiles = listOf(profile("two"), profile("one")),
            credentials = listOf(credential("two"), credential("one")),
        )

        val encoded = HostessVaultFileCodec.encode(plaintext)
        val decoded = assertIs<HostessVaultCodecResult.Decoded>(HostessVaultFileCodec.decode(encoded)).plaintext
        val encodedAgain = HostessVaultFileCodec.encode(decoded)

        assertEquals(listOf("profile:v1:one", "profile:v1:two"), decoded.profiles.map { it.profileId.value })
        assertEquals(
            listOf("hostess-vault:v1:one", "hostess-vault:v1:two"),
            decoded.credentials.map { it.credentialHandle.value },
        )
        assertContentEquals(encoded, encodedAgain)
        assertEquals("venue-password-one", decoded.credentials.first().sharedSecret.revealForLogin())
    }

    @Test
    fun `credential record string form redacts shared secret`() {
        val credential = credential("one")

        assertFalse(credential.toString().contains("venue-password-one"), credential.toString())
        assertEquals("[redacted]", assertNotNull(SharedSecret.fromPlainText("venue-password-one")).toString())
    }

    @Test
    fun `rejects duplicate profile id`() {
        val payload = HostessVaultFileCodec.encode(
            HostessVaultPlaintext(
                profiles = listOf(profile("one"), profile("one")),
                credentials = listOf(credential("one")),
            ),
        )

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects duplicate credential handle`() {
        val payload = HostessVaultFileCodec.encode(
            HostessVaultPlaintext(
                profiles = listOf(profile("one")),
                credentials = listOf(credential("one"), credential("one")),
            ),
        )

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects profile pointing to missing credential`() {
        val payload = HostessVaultFileCodec.encode(
            HostessVaultPlaintext(
                profiles = listOf(profile("one")),
                credentials = listOf(credential("two")),
            ),
        )

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects invalid credential handle prefix`() {
        val payload = HostessVaultFileCodec.encode(
            HostessVaultPlaintext(
                profiles = listOf(profile("one", credentialHandle = "credential-bad")),
                credentials = listOf(credential("one", credentialHandle = "credential-bad")),
            ),
        )

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects blank credential handle suffix`() {
        val payload = HostessVaultFileCodec.encode(
            HostessVaultPlaintext(
                profiles = listOf(
                    profile("one", credentialHandle = CredentialHandle.HOSTESS_VAULT_CREDENTIAL_PREFIX),
                ),
                credentials = listOf(
                    credential("one", credentialHandle = CredentialHandle.HOSTESS_VAULT_CREDENTIAL_PREFIX),
                ),
            ),
        )

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects bad payload version and trailing bytes`() {
        val payload = HostessVaultFileCodec.encode(validPlaintext())
        val badVersion = payload.copyOf().also { it[1] = 2 }
        val trailing = payload + byteArrayOf(0)

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(badVersion))
        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(trailing))
    }

    @Test
    fun `rejects invalid profile id and noncanonical login name`() {
        val payload = HostessVaultFileCodec.encode(validPlaintext())
        val invalidProfile = payload.replaceAscii("profile:v1:one", "badfile:v1:one")
        val invalidLoginName = payload.replaceAscii("jackraybold resident", "JackRaybold Resident")

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(invalidProfile))
        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(invalidLoginName))
    }

    @Test
    fun `rejects invalid utf8 string body`() {
        val payload = HostessVaultFileCodec.encode(validPlaintext())
        val mutated = payload.copyOf()
        val loginNameStart = mutated.indexOf("jackraybold resident".encodeToByteArray())
        mutated[loginNameStart] = 0xC3.toByte()
        mutated[loginNameStart + 1] = 0x28.toByte()

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(mutated))
    }

    @Test
    fun `rejects null marker in required string and overlong string`() {
        val payload = HostessVaultFileCodec.encode(validPlaintext())
        val profileIdLengthOffset = 10
        val nullProfileId = payload.copyOf().also {
            it[profileIdLengthOffset] = 0xFF.toByte()
            it[profileIdLengthOffset + 1] = 0xFF.toByte()
            it[profileIdLengthOffset + 2] = 0xFF.toByte()
            it[profileIdLengthOffset + 3] = 0xFF.toByte()
        }
        val overlongProfileId = payload.copyOf().also {
            val overlong = VAULT_MAX_STRING_BYTES + 1
            it[profileIdLengthOffset] = ((overlong ushr 24) and 0xFF).toByte()
            it[profileIdLengthOffset + 1] = ((overlong ushr 16) and 0xFF).toByte()
            it[profileIdLengthOffset + 2] = ((overlong ushr 8) and 0xFF).toByte()
            it[profileIdLengthOffset + 3] = (overlong and 0xFF).toByte()
        }

        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(nullProfileId))
        assertIs<HostessVaultCodecResult.Corrupt>(HostessVaultFileCodec.decode(overlongProfileId))
    }

    private fun validPlaintext(): HostessVaultPlaintext =
        HostessVaultPlaintext(profiles = listOf(profile("one")), credentials = listOf(credential("one")))

    private fun profile(
        suffix: String,
        credentialHandle: String = "hostess-vault:v1:$suffix",
    ): HostessVaultProfileRecord = HostessVaultProfileRecord(
        profileId = AccountProfileId("profile:v1:$suffix"),
        loginName = loginName("jackraybold"),
        label = "jackraybold resident",
        credentialHandle = CredentialHandle(credentialHandle),
        startLocation = "uri:London City&76&174&23",
    )

    private fun credential(
        suffix: String,
        credentialHandle: String = "hostess-vault:v1:$suffix",
    ): HostessVaultCredentialRecord = HostessVaultCredentialRecord(
        credentialHandle = CredentialHandle(credentialHandle),
        loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
        loginName = loginName("jackraybold"),
        sharedSecret = assertNotNull(SharedSecret.fromPlainText("venue-password-$suffix")),
        startLocation = "uri:London City&76&174&23",
    )

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName

    private fun ByteArray.replaceAscii(oldValue: String, newValue: String): ByteArray {
        require(oldValue.length == newValue.length)
        val oldBytes = oldValue.encodeToByteArray()
        val newBytes = newValue.encodeToByteArray()
        val copy = copyOf()
        val start = copy.indexOf(oldBytes)
        require(start >= 0)
        newBytes.copyInto(copy, destinationOffset = start)
        return copy
    }

    private fun ByteArray.indexOf(needle: ByteArray): Int {
        for (index in 0..(size - needle.size)) {
            var found = true
            for (needleIndex in needle.indices) {
                if (this[index + needleIndex] != needle[needleIndex]) {
                    found = false
                    break
                }
            }
            if (found) {
                return index
            }
        }
        return -1
    }
}
