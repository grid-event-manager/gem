package org.gem.credential.vault

import org.gem.core.domain.AccountProfileId
import org.gem.core.domain.SecondLifeLoginName
import org.gem.core.domain.SecondLifeLoginNameResult
import org.gem.core.domain.SecondLifeLoginUri
import org.gem.core.domain.SharedSecret
import org.gem.core.ports.CredentialHandle
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class GemVaultFileCodecTest {
    @Test
    fun `round trips deterministically and sorts records`() {
        val plaintext = GemVaultPlaintext(
            profiles = listOf(profile("two"), profile("one")),
            credentials = listOf(credential("two"), credential("one")),
        )

        val encoded = GemVaultFileCodec.encode(plaintext)
        val decoded = assertIs<GemVaultCodecResult.Decoded>(GemVaultFileCodec.decode(encoded)).plaintext
        val encodedAgain = GemVaultFileCodec.encode(decoded)

        assertEquals(listOf("profile:v1:one", "profile:v1:two"), decoded.profiles.map { it.profileId.value })
        assertEquals(
            listOf("gem-vault:v1:one", "gem-vault:v1:two"),
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
        val payload = GemVaultFileCodec.encode(
            GemVaultPlaintext(
                profiles = listOf(profile("one"), profile("one")),
                credentials = listOf(credential("one")),
            ),
        )

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects duplicate credential handle`() {
        val payload = GemVaultFileCodec.encode(
            GemVaultPlaintext(
                profiles = listOf(profile("one")),
                credentials = listOf(credential("one"), credential("one")),
            ),
        )

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects profile pointing to missing credential`() {
        val payload = GemVaultFileCodec.encode(
            GemVaultPlaintext(
                profiles = listOf(profile("one")),
                credentials = listOf(credential("two")),
            ),
        )

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects invalid credential handle prefix`() {
        val payload = GemVaultFileCodec.encode(
            GemVaultPlaintext(
                profiles = listOf(profile("one", credentialHandle = "credential-bad")),
                credentials = listOf(credential("one", credentialHandle = "credential-bad")),
            ),
        )

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects blank credential handle suffix`() {
        val payload = GemVaultFileCodec.encode(
            GemVaultPlaintext(
                profiles = listOf(
                    profile("one", credentialHandle = CredentialHandle.HOSTESS_VAULT_CREDENTIAL_PREFIX),
                ),
                credentials = listOf(
                    credential("one", credentialHandle = CredentialHandle.HOSTESS_VAULT_CREDENTIAL_PREFIX),
                ),
            ),
        )

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(payload))
    }

    @Test
    fun `rejects bad payload version and trailing bytes`() {
        val payload = GemVaultFileCodec.encode(validPlaintext())
        val badVersion = payload.copyOf().also { it[1] = 2 }
        val trailing = payload + byteArrayOf(0)

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(badVersion))
        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(trailing))
    }

    @Test
    fun `rejects invalid profile id and noncanonical login name`() {
        val payload = GemVaultFileCodec.encode(validPlaintext())
        val invalidProfile = payload.replaceAscii("profile:v1:one", "badfile:v1:one")
        val invalidLoginName = payload.replaceAscii("venuehost resident", "VenueHost Resident")

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(invalidProfile))
        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(invalidLoginName))
    }

    @Test
    fun `rejects invalid utf8 string body`() {
        val payload = GemVaultFileCodec.encode(validPlaintext())
        val mutated = payload.copyOf()
        val loginNameStart = mutated.indexOf("venuehost resident".encodeToByteArray())
        mutated[loginNameStart] = 0xC3.toByte()
        mutated[loginNameStart + 1] = 0x28.toByte()

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(mutated))
    }

    @Test
    fun `rejects null marker in required string and overlong string`() {
        val payload = GemVaultFileCodec.encode(validPlaintext())
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

        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(nullProfileId))
        assertIs<GemVaultCodecResult.Corrupt>(GemVaultFileCodec.decode(overlongProfileId))
    }

    private fun validPlaintext(): GemVaultPlaintext =
        GemVaultPlaintext(profiles = listOf(profile("one")), credentials = listOf(credential("one")))

    private fun profile(
        suffix: String,
        credentialHandle: String = "gem-vault:v1:$suffix",
    ): GemVaultProfileRecord = GemVaultProfileRecord(
        profileId = AccountProfileId("profile:v1:$suffix"),
        loginName = loginName("venuehost"),
        label = "venuehost resident",
        credentialHandle = CredentialHandle(credentialHandle),
        startLocation = "uri:London City&76&174&23",
    )

    private fun credential(
        suffix: String,
        credentialHandle: String = "gem-vault:v1:$suffix",
    ): GemVaultCredentialRecord = GemVaultCredentialRecord(
        credentialHandle = CredentialHandle(credentialHandle),
        loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
        loginName = loginName("venuehost"),
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
