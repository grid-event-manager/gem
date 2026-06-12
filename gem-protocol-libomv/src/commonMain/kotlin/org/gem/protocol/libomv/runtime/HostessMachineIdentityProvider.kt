package org.gem.protocol.libomv.runtime

fun interface HostessMachineIdentityProvider {
    fun resolve(): HostessMachineIdentity
}
