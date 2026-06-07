package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.SimulatorCircuit
import org.hostess.protocol.libomv.transport.SimulatorCircuitSendResult

internal class ProtocolNoticeCircuitSource(
    private val circuitClient: ProtocolSimulatorCircuitClient,
) : NoticeRuntimeSource {
    override fun send(identity: LibomvSessionIdentity, packet: LibomvNoticePacket): NoticeRuntimeResult =
        when (val result = circuitClient.sendNotice(identity.toCircuit(), packet)) {
            SimulatorCircuitSendResult.Sent -> NoticeRuntimeResult.Sent
            is SimulatorCircuitSendResult.Failed -> NoticeRuntimeResult.Failed(result.redactedMessage)
        }

    private fun LibomvSessionIdentity.toCircuit(): SimulatorCircuit = SimulatorCircuit(
        agentId = agentId,
        sessionId = sessionId,
        seedCapability = seedCapability,
        simulatorIp = simulatorIp,
        simulatorPort = simulatorPort,
        regionHandle = regionHandle,
        circuitCode = circuitCode,
    )
}
