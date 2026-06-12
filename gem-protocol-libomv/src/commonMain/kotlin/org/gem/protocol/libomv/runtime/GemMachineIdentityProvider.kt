package org.gem.protocol.libomv.runtime

fun interface GemMachineIdentityProvider {
    fun resolve(): GemMachineIdentity
}
