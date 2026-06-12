package org.gem.protocol.libomv.runtime

internal fun interface Md5DigestPort {
    fun md5Hex(vararg chunks: ByteArray): String
}
