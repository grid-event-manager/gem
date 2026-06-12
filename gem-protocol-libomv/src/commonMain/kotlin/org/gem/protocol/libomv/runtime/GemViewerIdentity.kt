package org.gem.protocol.libomv.runtime

data class GemViewerIdentity(
    val channel: String,
    val version: String,
    val author: String,
    val platform: GemPlatformIdentity,
    val host: GemHostIdentity,
)
