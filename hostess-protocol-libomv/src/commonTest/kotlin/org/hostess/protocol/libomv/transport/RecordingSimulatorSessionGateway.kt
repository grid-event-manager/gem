package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvNoticePacket

internal class RecordingSimulatorSessionGateway(
    var presenceResult: SimulatorPresenceResult = SimulatorPresenceResult.Present(
        pingReplies = 0,
        cached = false,
    ),
    var currentGroupsResult: SimulatorCircuitSendResult = SimulatorCircuitSendResult.Sent(),
    var noticeResult: SimulatorCircuitSendResult = SimulatorCircuitSendResult.Sent(),
    var archiveResult: SimulatorNoticeArchiveResult = SimulatorNoticeArchiveResult.Found(emptyList()),
    var logoutResult: SimulatorLogoutResult = LoggedOut,
    var healthResult: SimulatorSessionHealth = SimulatorSessionHealth(SimulatorSessionHealthStatus.PRESENT),
) : SimulatorSessionGateway {
    val presenceCircuits = mutableListOf<SimulatorCircuit>()
    val currentGroupsCircuits = mutableListOf<SimulatorCircuit>()
    val noticeCircuits = mutableListOf<SimulatorCircuit>()
    val noticePackets = mutableListOf<LibomvNoticePacket>()
    val archiveCircuits = mutableListOf<SimulatorCircuit>()
    val archiveGroupIds = mutableListOf<String>()
    val logoutCircuits = mutableListOf<SimulatorCircuit>()
    val healthCircuits = mutableListOf<SimulatorCircuit>()

    override fun ensurePresence(circuit: SimulatorCircuit): SimulatorPresenceResult {
        presenceCircuits += circuit
        return presenceResult
    }

    override fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult {
        currentGroupsCircuits += circuit
        return currentGroupsResult
    }

    override fun sendNotice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorCircuitSendResult {
        noticeCircuits += circuit
        noticePackets += packet
        return noticeResult
    }

    override fun requestGroupNoticeArchive(
        circuit: SimulatorCircuit,
        groupId: String,
    ): SimulatorNoticeArchiveResult {
        archiveCircuits += circuit
        archiveGroupIds += groupId
        return archiveResult
    }

    override fun logout(circuit: SimulatorCircuit): SimulatorLogoutResult {
        logoutCircuits += circuit
        return logoutResult
    }

    override fun health(circuit: SimulatorCircuit): SimulatorSessionHealth {
        healthCircuits += circuit
        return healthResult
    }
}
