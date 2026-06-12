package org.gem.protocol.libomv.runtime

fun interface GemViewerIdentityProvider {
    fun resolve(): GemViewerIdentity
}
