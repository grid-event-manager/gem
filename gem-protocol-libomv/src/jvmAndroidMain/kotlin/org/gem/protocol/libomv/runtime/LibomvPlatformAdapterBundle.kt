package org.gem.protocol.libomv.runtime

import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant
import org.gem.core.ports.ClockPort
import org.gem.protocol.libomv.transport.OkHttpProtocolHttpClient
import org.gem.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.gem.protocol.libomv.transport.ProtocolHttpClient
import org.gem.protocol.libomv.transport.SimulatorPacketExchangeFactory
import org.gem.protocol.libomv.transport.ThreadedSimulatorSessionGateway
import org.gem.protocol.libomv.transport.UdpSimulatorDatagramSender

internal data class LibomvPlatformAdapterBundle(
    val httpClient: ProtocolHttpClient,
    val secretResolver: LoginSecretResolver,
    val viewerIdentityProvider: GemViewerIdentityProvider,
    val machineIdentityProvider: GemMachineIdentityProvider,
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
            viewerIdentityProvider = DefaultGemViewerIdentityProvider,
            machineIdentityProvider = DefaultGemMachineIdentityProvider,
            clockPort = JvmProtocolClockPort,
            md5DigestPort = JvmMd5DigestPort,
            circuitSender = ProtocolSimulatorCircuitClient(
                ThreadedSimulatorSessionGateway(
                    SimulatorPacketExchangeFactory { UdpSimulatorDatagramSender() },
                ),
            ),
            adapterLoad = true,
            runtimeLoad = true,
            transportLoad = true,
        )
}

private object JvmProtocolClockPort : ClockPort {
    override fun now(): GemInstant =
        GemInstant(System.currentTimeMillis())

    override fun pause(duration: GemDelay) {
        Thread.sleep(duration.milliseconds)
    }
}
