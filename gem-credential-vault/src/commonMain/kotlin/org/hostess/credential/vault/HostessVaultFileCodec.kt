package org.hostess.credential.vault

import org.hostess.core.domain.AccountProfileId
import org.hostess.core.domain.SecondLifeLoginName
import org.hostess.core.domain.SecondLifeLoginNameResult
import org.hostess.core.domain.SecondLifeLoginUri
import org.hostess.core.domain.SharedSecret
import org.hostess.core.ports.CredentialHandle

object HostessVaultFileCodec {
    fun encode(plaintext: HostessVaultPlaintext): ByteArray {
        require(plaintext.payloadVersion == VAULT_PAYLOAD_VERSION) { "Unsupported vault payload version." }
        val writer = VaultPayloadWriter()
        writer.writeUShort(VAULT_PAYLOAD_VERSION.toInt())
        writer.writeUInt(plaintext.profiles.size)
        writer.writeUInt(plaintext.credentials.size)
        plaintext.profiles.sortedBy { it.profileId.value }.forEach(writer::writeProfile)
        plaintext.credentials.sortedBy { it.credentialHandle.value }.forEach(writer::writeCredential)
        return writer.toByteArray()
    }

    fun decode(payloadBytes: ByteArray): HostessVaultCodecResult {
        val reader = VaultPayloadReader(payloadBytes)
        val payloadVersion = reader.readUShort() ?: return corrupt()
        if (payloadVersion != VAULT_PAYLOAD_VERSION.toInt()) {
            return corrupt("bad_payload_version")
        }

        val profileCount = reader.readRecordCount() ?: return corrupt()
        val credentialCount = reader.readRecordCount() ?: return corrupt()
        val profiles = List(profileCount) {
            reader.readProfile() ?: return corrupt(reader.message)
        }
        val credentials = List(credentialCount) {
            reader.readCredential() ?: return corrupt(reader.message)
        }
        if (reader.hasRemaining()) {
            return corrupt("trailing_bytes")
        }

        return validate(HostessVaultPlaintext(profiles = profiles, credentials = credentials))
    }

    private fun validate(plaintext: HostessVaultPlaintext): HostessVaultCodecResult {
        val profileIds = mutableSetOf<String>()
        val credentialHandles = mutableSetOf<String>()

        plaintext.profiles.forEach { profile ->
            if (!profileIds.add(profile.profileId.value)) {
                return corrupt("duplicate_profile_id")
            }
        }
        plaintext.credentials.forEach { credential ->
            if (!credential.credentialHandle.isHostessVaultCredentialHandle()) {
                return corrupt("invalid_credential_handle")
            }
            if (!credentialHandles.add(credential.credentialHandle.value)) {
                return corrupt("duplicate_credential_handle")
            }
        }
        plaintext.profiles.forEach { profile ->
            if (profile.credentialHandle.value !in credentialHandles) {
                return corrupt("missing_profile_credential")
            }
        }

        return HostessVaultCodecResult.Decoded(plaintext)
    }

    private fun corrupt(message: String? = "corrupt"): HostessVaultCodecResult.Corrupt =
        HostessVaultCodecResult.Corrupt(message)
}

sealed interface HostessVaultCodecResult {
    data class Decoded(val plaintext: HostessVaultPlaintext) : HostessVaultCodecResult
    data class Corrupt(val message: String? = null) : HostessVaultCodecResult
}

private class VaultPayloadWriter {
    private val bytes = mutableListOf<Byte>()

    fun writeProfile(profile: HostessVaultProfileRecord) {
        writeString(profile.profileId.value)
        writeString(profile.loginName.value)
        writeString(profile.label)
        writeString(profile.credentialHandle.value)
        writeNullableString(profile.startLocation)
    }

    fun writeCredential(credential: HostessVaultCredentialRecord) {
        writeString(credential.credentialHandle.value)
        writeString(credential.loginUri.value)
        writeString(credential.loginName.value)
        writeString(credential.sharedSecret.revealForLogin())
        writeNullableString(credential.startLocation)
    }

    fun writeUShort(value: Int) {
        require(value in 0..0xFFFF) { "Unsigned short out of range." }
        bytes += ((value ushr 8) and 0xFF).toByte()
        bytes += (value and 0xFF).toByte()
    }

    fun writeUInt(value: Int) {
        require(value >= 0) { "Unsigned int cannot be negative." }
        bytes += ((value ushr 24) and 0xFF).toByte()
        bytes += ((value ushr 16) and 0xFF).toByte()
        bytes += ((value ushr 8) and 0xFF).toByte()
        bytes += (value and 0xFF).toByte()
    }

    private fun writeString(value: String) {
        val encoded = value.encodeToByteArray()
        require(encoded.size <= VAULT_MAX_STRING_BYTES) { "String exceeds vault codec limit." }
        writeUInt(encoded.size)
        encoded.forEach { bytes += it }
    }

    private fun writeNullableString(value: String?) {
        if (value == null) {
            repeat(4) { bytes += 0xFF.toByte() }
            return
        }
        writeString(value)
    }

    fun toByteArray(): ByteArray = bytes.toByteArray()
}

private class VaultPayloadReader(
    private val bytes: ByteArray,
) {
    var message: String? = null
        private set
    private var offset: Int = 0

    fun readProfile(): HostessVaultProfileRecord? {
        val profileId = readProfileId() ?: return null
        val loginName = readLoginName() ?: return null
        val label = readString() ?: return null
        if (label.isBlank()) {
            return fail("blank_profile_label")
        }
        val credentialHandle = readCredentialHandle() ?: return null
        val startLocation = when (val read = readNullableString()) {
            NullableStringRead.Failed -> return null
            is NullableStringRead.Value -> read.value
        }
        return HostessVaultProfileRecord(profileId, loginName, label, credentialHandle, startLocation)
    }

    fun readCredential(): HostessVaultCredentialRecord? {
        val credentialHandle = readCredentialHandle() ?: return null
        val loginUri = readLoginUri() ?: return null
        val loginName = readLoginName() ?: return null
        val secretText = readString() ?: return null
        val sharedSecret = SharedSecret.fromPlainText(secretText) ?: return fail("blank_shared_secret")
        val startLocation = when (val read = readNullableString()) {
            NullableStringRead.Failed -> return null
            is NullableStringRead.Value -> read.value
        }
        return HostessVaultCredentialRecord(credentialHandle, loginUri, loginName, sharedSecret, startLocation)
    }

    fun readRecordCount(): Int? {
        val count = readUInt() ?: return null
        if (count > VAULT_MAX_RECORD_COUNT.toUInt()) {
            return fail("record_count_too_large")
        }
        return count.toInt()
    }

    fun readUShort(): Int? {
        if (remaining() < 2) {
            return fail("truncated_uint16")
        }
        val value = (bytes[offset].unsigned() shl 8) or bytes[offset + 1].unsigned()
        offset += 2
        return value
    }

    fun hasRemaining(): Boolean = offset != bytes.size

    private fun readProfileId(): AccountProfileId? {
        val value = readString() ?: return null
        return try {
            AccountProfileId(value)
        } catch (_: IllegalArgumentException) {
            fail("invalid_profile_id")
        }
    }

    private fun readCredentialHandle(): CredentialHandle? {
        val value = readString() ?: return null
        return try {
            CredentialHandle(value)
        } catch (_: IllegalArgumentException) {
            fail("invalid_credential_handle")
        }
    }

    private fun readLoginUri(): SecondLifeLoginUri? {
        val value = readString() ?: return null
        return try {
            SecondLifeLoginUri(value)
        } catch (_: IllegalArgumentException) {
            fail("invalid_login_uri")
        }
    }

    private fun readLoginName(): SecondLifeLoginName? {
        val value = readString() ?: return null
        return when (val result = SecondLifeLoginName.fromUserInput(value)) {
            is SecondLifeLoginNameResult.Valid ->
                if (result.loginName.value == value) result.loginName else fail("noncanonical_login_name")
            is SecondLifeLoginNameResult.Invalid -> fail("invalid_login_name")
        }
    }

    private fun readNullableString(): NullableStringRead {
        val length = readUInt() ?: return NullableStringRead.Failed
        if (length == NULL_STRING_MARKER) {
            return NullableStringRead.Value(null)
        }
        if (length > VAULT_MAX_STRING_BYTES.toUInt()) {
            fail<Nothing>("string_too_large")
            return NullableStringRead.Failed
        }
        val value = readStringBody(length.toInt()) ?: return NullableStringRead.Failed
        return NullableStringRead.Value(value)
    }

    private fun readString(): String? {
        val length = readUInt() ?: return null
        if (length == NULL_STRING_MARKER) {
            return fail("null_marker_in_required_string")
        }
        if (length > VAULT_MAX_STRING_BYTES.toUInt()) {
            return fail("string_too_large")
        }
        return readStringBody(length.toInt())
    }

    private fun readStringBody(length: Int): String? {
        if (remaining() < length) {
            return fail("truncated_string")
        }
        val value = try {
            bytes.decodeToString(offset, offset + length, throwOnInvalidSequence = true)
        } catch (_: Throwable) {
            return fail("invalid_utf8")
        }
        offset += length
        return value
    }

    private fun readUInt(): UInt? {
        if (remaining() < 4) {
            return fail("truncated_uint32")
        }
        val value = (bytes[offset].unsigned().toUInt() shl 24) or
            (bytes[offset + 1].unsigned().toUInt() shl 16) or
            (bytes[offset + 2].unsigned().toUInt() shl 8) or
            bytes[offset + 3].unsigned().toUInt()
        offset += 4
        return value
    }

    private fun remaining(): Int = bytes.size - offset

    private fun <T> fail(reason: String): T? {
        message = reason
        return null
    }

    companion object {
        private val NULL_STRING_MARKER: UInt = UInt.MAX_VALUE
    }
}

private fun Byte.unsigned(): Int = toInt() and 0xFF

private sealed interface NullableStringRead {
    data class Value(val value: String?) : NullableStringRead
    data object Failed : NullableStringRead
}
