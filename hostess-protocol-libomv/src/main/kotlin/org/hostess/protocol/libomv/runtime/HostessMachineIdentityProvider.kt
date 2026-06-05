package org.hostess.protocol.libomv.runtime

fun interface HostessMachineIdentityProvider {
    fun resolve(): HostessMachineIdentity
}
