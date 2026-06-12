package org.gem.protocol.libomv.runtime

import org.gem.protocol.libomv.LibomvSessionIdentity
import org.gem.protocol.libomv.mapping.LibomvNoticePacket
import org.gem.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.gem.protocol.libomv.transport.SimulatorCircuitSendResult
import org.gem.protocol.libomv.transport.toSimulatorCircuit

internal class ProtocolNoticeCircuitSource(
    private val circuitClient: ProtocolSimulatorCircuitClient,
) : NoticeRuntimeSource {
    override fun send(identity: LibomvSessionIdentity, packet: LibomvNoticePacket): NoticeRuntimeResult =
        when (val result = circuitClient.sendNotice(identity.toSimulatorCircuit(), packet)) {
            is SimulatorCircuitSendResult.Sent -> NoticeRuntimeResult.Sent(result.redactedDetail)
            is SimulatorCircuitSendResult.Failed -> NoticeRuntimeResult.Failed(result.redactedMessage)
        }
}
