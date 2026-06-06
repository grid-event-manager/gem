package org.hostess.protocol.libomv.runtime

fun interface HostessViewerIdentityProvider {
    fun resolve(): HostessViewerIdentity
}
