package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvNoticePacket

internal interface SimulatorSessionGateway {
    fun ensurePresence(circuit: SimulatorCircuit): SimulatorPresenceResult
    fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult
    fun sendNotice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorCircuitSendResult
    fun requestGroupNoticeArchive(circuit: SimulatorCircuit, groupId: String): SimulatorNoticeArchiveResult
    fun logout(circuit: SimulatorCircuit): SimulatorLogoutResult
    fun health(circuit: SimulatorCircuit): SimulatorSessionHealth
}
