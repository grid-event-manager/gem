package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant
import org.hostess.core.ports.ClockPort
import org.hostess.protocol.libomv.transport.OkHttpProtocolHttpClient
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.ProtocolHttpClient
import org.hostess.protocol.libomv.transport.UdpSimulatorDatagramSender

internal data class LibomvPlatformAdapterBundle(
    val httpClient: ProtocolHttpClient,
    val secretResolver: LoginSecretResolver,
    val viewerIdentityProvider: HostessViewerIdentityProvider,
    val machineIdentityProvider: HostessMachineIdentityProvider,
    val clockPort: ClockPort,
    val md5DigestPort: Md5DigestPort,
    val circuitSender: ProtocolSimulatorCircuitClient,
    val adapterLoad: Boolean,
    val runtimeLoad: Boolean,
    val transportLoad: Boolean,
)

internal object DefaultLibomvPlatformAdapterBundle {
    fun create(
        secretResolver: LoginSecretResolver = EnvironmentLoginSecretResolver(),
    ): LibomvPlatformAdapterBundle =
        LibomvPlatformAdapterBundle(
            httpClient = OkHttpProtocolHttpClient(),
            secretResolver = secretResolver,
            viewerIdentityProvider = DefaultHostessViewerIdentityProvider,
            machineIdentityProvider = DefaultHostessMachineIdentityProvider,
            clockPort = JvmProtocolClockPort,
            md5DigestPort = JvmMd5DigestPort,
            circuitSender = ProtocolSimulatorCircuitClient(UdpSimulatorDatagramSender()),
            adapterLoad = true,
            runtimeLoad = true,
            transportLoad = true,
        )
}

private object JvmProtocolClockPort : ClockPort {
    override fun now(): HostessInstant =
        HostessInstant(System.currentTimeMillis())

    override fun pause(duration: HostessDelay) {
        Thread.sleep(duration.milliseconds)
    }
}
