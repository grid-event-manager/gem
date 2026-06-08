package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.SimulatorCircuitSendResult
import org.hostess.protocol.libomv.transport.toSimulatorCircuit

internal class ProtocolNoticeCircuitSource(
    private val circuitClient: ProtocolSimulatorCircuitClient,
) : NoticeRuntimeSource {
    override fun send(identity: LibomvSessionIdentity, packet: LibomvNoticePacket): NoticeRuntimeResult =
        when (val result = circuitClient.sendNotice(identity.toSimulatorCircuit(), packet)) {
            is SimulatorCircuitSendResult.Sent -> NoticeRuntimeResult.Sent(result.redactedDetail)
            is SimulatorCircuitSendResult.Failed -> NoticeRuntimeResult.Failed(result.redactedMessage)
        }
}
