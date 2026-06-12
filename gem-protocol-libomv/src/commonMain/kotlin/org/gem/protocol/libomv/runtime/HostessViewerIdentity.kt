package org.gem.protocol.libomv.runtime

data class HostessViewerIdentity(
    val channel: String,
    val version: String,
    val author: String,
    val platform: HostessPlatformIdentity,
    val host: HostessHostIdentity,
)
