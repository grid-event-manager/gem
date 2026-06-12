package org.gem.protocol.libomv.runtime

data class GemHostIdentity(
    val mac: String,
    val id0: String,
    val hostId: String,
) {
    init {
        require(HOST_ID_PATTERN.matches(mac)) { "mac must be a redacted 32-character digest." }
        require(HOST_ID_PATTERN.matches(id0)) { "id0 must be a redacted 32-character digest." }
        require(HOST_ID_PATTERN.matches(hostId)) { "hostId must be a redacted 32-character digest." }
    }

    private companion object {
        val HOST_ID_PATTERN = Regex("[0-9a-f]{32}")
    }
}
