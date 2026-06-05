package org.hostess.protocol.libomv.runtime

import java.security.MessageDigest

internal object JvmMd5DigestPort : Md5DigestPort {
    override fun md5Hex(vararg chunks: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        chunks.forEach(digest::update)
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = buildString(size * 2) {
        this@toHex.forEach { byte ->
            val value = byte.toInt() and BYTE_MASK
            append(HEX[value ushr 4])
            append(HEX[value and NIBBLE_MASK])
        }
    }

    private const val BYTE_MASK = 0xFF
    private const val NIBBLE_MASK = 0x0F
    private const val HEX = "0123456789abcdef"
}
