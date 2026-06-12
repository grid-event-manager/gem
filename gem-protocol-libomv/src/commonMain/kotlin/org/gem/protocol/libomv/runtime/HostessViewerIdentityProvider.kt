package org.gem.protocol.libomv.runtime

fun interface HostessViewerIdentityProvider {
    fun resolve(): HostessViewerIdentity
}
