package org.hostess.protocol.libomv.transport

import org.hostess.protocol.libomv.mapping.LibomvNoticePacket
import org.hostess.protocol.libomv.mapping.LibomvUuidCodec

internal data class SimulatorCircuit(
    val agentId: String,
    val sessionId: String,
    val seedCapability: String,
    val simulatorIp: String,
    val simulatorPort: Int,
    val regionHandle: Long,
    val circuitCode: Long,
)

internal sealed interface SimulatorCircuitSendResult {
    data class Sent(val redactedDetail: String? = null) : SimulatorCircuitSendResult
    data class Failed(val redactedMessage: String) : SimulatorCircuitSendResult
}

internal class ProtocolSimulatorCircuitClient(
    private val gateway: SimulatorSessionGateway,
) {
    fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult =
        if (circuit.isUsable()) {
            gateway.sendCurrentGroupsRequest(circuit)
        } else {
            SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }

    fun sendNotice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorCircuitSendResult =
        if (circuit.isUsable()) {
            gateway.sendNotice(circuit, packet)
        } else {
            SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
        }

    fun requestGroupNoticeArchive(circuit: SimulatorCircuit, groupId: String): SimulatorNoticeArchiveResult {
        val canonicalGroupId = LibomvUuidCodec.canonicalOrNull(groupId)
        return if (circuit.isUsable() && canonicalGroupId != null) {
            gateway.requestGroupNoticeArchive(circuit, canonicalGroupId)
        } else {
            SimulatorNoticeArchiveResult.Failed(
                status = SimulatorNoticeArchiveStatus.REQUEST_INVALID,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }
    }

    fun ensurePresence(circuit: SimulatorCircuit): SimulatorPresenceResult =
        if (circuit.isUsable()) {
            gateway.ensurePresence(circuit)
        } else {
            SimulatorPresenceResult.Failed(
                status = SimulatorPresenceStatus.CIRCUIT_INVALID,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }

    fun logout(circuit: SimulatorCircuit): SimulatorLogoutResult =
        if (circuit.isUsable()) {
            gateway.logout(circuit)
        } else {
            Failed(REDACTED_SEND_FAILURE)
        }

    fun health(circuit: SimulatorCircuit): SimulatorSessionHealth =
        if (circuit.isUsable()) {
            gateway.health(circuit)
        } else {
            SimulatorSessionHealth(SimulatorSessionHealthStatus.FAILED, REDACTED_SEND_FAILURE)
        }

    private fun SimulatorCircuit.isUsable(): Boolean =
        agentId.isUuid() &&
            sessionId.isUuid() &&
            seedCapability.isNotBlank() &&
            simulatorIp.isIpv4Address() &&
            simulatorPort in 1..MAX_PORT &&
            circuitCode in 1..UNSIGNED_32_MAX

    private fun String.isUuid(): Boolean =
        LibomvUuidCodec.packetBytes(this) != null

    private fun String.isIpv4Address(): Boolean {
        val parts = split(".")
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() &&
                part.all(Char::isDigit) &&
                part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    private companion object {
        const val REDACTED_SEND_FAILURE: String = "protocol simulator send failed"
        const val MAX_PORT: Int = 65535
        const val UNSIGNED_32_MAX: Long = 0xFFFF_FFFFL
    }
}
