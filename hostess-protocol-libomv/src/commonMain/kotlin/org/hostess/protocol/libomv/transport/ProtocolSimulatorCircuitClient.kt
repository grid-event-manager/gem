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
    data object Sent : SimulatorCircuitSendResult
    data class Failed(val redactedMessage: String) : SimulatorCircuitSendResult
}

internal class ProtocolSimulatorCircuitClient(
    private val packetExchange: SimulatorPacketExchange,
    private val sequence: SimulatorPacketSequence = SimulatorPacketSequence(),
) {
    private var presentCircuit: SimulatorCircuitKey? = null

    fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult =
        sendAfterPresence(circuit) {
            LibomvPacketCodec.agentDataUpdateRequest(circuit, sequence.next())
        }

    fun sendNotice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorCircuitSendResult =
        sendAfterPresence(circuit) {
            LibomvNoticePacketCodec.improvedInstantMessage(packet, sequence.next())
        }

    fun ensurePresence(circuit: SimulatorCircuit): SimulatorPresenceResult {
        if (!circuit.isUsable()) {
            return SimulatorPresenceResult.Failed(
                status = SimulatorPresenceStatus.CIRCUIT_INVALID,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }
        val circuitKey = circuit.toKey()
        if (presentCircuit == circuitKey) {
            return SimulatorPresenceResult.Present(pingReplies = 0, cached = true)
        }
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)

        return try {
            sendPresencePacket(endpoint, SimulatorPresenceStatus.USE_CIRCUIT_CODE_FAILED) {
                LibomvPacketCodec.useCircuitCode(circuit, sequence.next())
            }?.let { return it }
            val handshake = when (
                val result = waitForPacket(
                    endpoint = endpoint,
                    receiveTimeoutMillis = HANDSHAKE_RECEIVE_TIMEOUT_MILLIS,
                    maxAttempts = HANDSHAKE_RECEIVE_ATTEMPTS,
                    wantedType = SimulatorPacketType.REGION_HANDSHAKE,
                    timeoutStatus = SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
                )
            ) {
                is WaitForPacketResult.Failed -> return result.toPresenceFailure()
                is WaitForPacketResult.Found -> result
            }
            sendPresencePacket(endpoint, SimulatorPresenceStatus.HANDSHAKE_REPLY_FAILED) {
                LibomvPacketCodec.regionHandshakeReply(circuit, sequence.next())
            }?.let { return it }
            sendPresencePacket(endpoint, SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED) {
                LibomvPacketCodec.completeAgentMovement(circuit, sequence.next())
            }?.let { return it }
            val movement = when (
                val result = waitForPacket(
                    endpoint = endpoint,
                    receiveTimeoutMillis = MOVEMENT_RECEIVE_TIMEOUT_MILLIS,
                    maxAttempts = MOVEMENT_RECEIVE_ATTEMPTS,
                    wantedType = SimulatorPacketType.AGENT_MOVEMENT_COMPLETE,
                    timeoutStatus = SimulatorPresenceStatus.MOVEMENT_TIMEOUT,
                    pingReplies = handshake.pingReplies,
                )
            ) {
                is WaitForPacketResult.Failed -> return result.toPresenceFailure()
                is WaitForPacketResult.Found -> result
            }
            sendPresencePacket(endpoint, SimulatorPresenceStatus.AGENT_UPDATE_FAILED) {
                LibomvPacketCodec.agentUpdate(circuit, sequence.next())
            }?.let { return it }
            presentCircuit = circuitKey
            SimulatorPresenceResult.Present(pingReplies = movement.pingReplies, cached = false)
        } catch (ex: IllegalArgumentException) {
            SimulatorPresenceResult.Failed(
                status = SimulatorPresenceStatus.HANDSHAKE_MALFORMED,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        } catch (ex: Exception) {
            SimulatorPresenceResult.Failed(
                status = SimulatorPresenceStatus.SEND_FAILED,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }
    }

    private fun sendPresencePacket(
        endpoint: SimulatorEndpoint,
        failureStatus: SimulatorPresenceStatus,
        payload: () -> ByteArray,
    ): SimulatorPresenceResult.Failed? =
        try {
            packetExchange.send(endpoint, listOf(payload()))
            null
        } catch (ex: Exception) {
            SimulatorPresenceResult.Failed(failureStatus, REDACTED_SEND_FAILURE)
        }

    private fun WaitForPacketResult.Failed.toPresenceFailure(): SimulatorPresenceResult.Failed =
        SimulatorPresenceResult.Failed(status, REDACTED_SEND_FAILURE)

    private fun sendAfterPresence(
        circuit: SimulatorCircuit,
        payload: () -> ByteArray,
    ): SimulatorCircuitSendResult {
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
        return when (val presence = ensurePresence(circuit)) {
            is SimulatorPresenceResult.Failed -> SimulatorCircuitSendResult.Failed(presence.redactedMessage)
            is SimulatorPresenceResult.Present -> try {
                packetExchange.send(endpoint, listOf(payload()))
                SimulatorCircuitSendResult.Sent
            } catch (ex: IllegalArgumentException) {
                SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
            } catch (ex: Exception) {
                SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
            }
        }
    }

    private fun waitForPacket(
        endpoint: SimulatorEndpoint,
        receiveTimeoutMillis: Int,
        maxAttempts: Int,
        wantedType: SimulatorPacketType,
        timeoutStatus: SimulatorPresenceStatus,
        pingReplies: Int = 0,
    ): WaitForPacketResult {
        var replies = pingReplies
        repeat(maxAttempts) {
            val inbound = packetExchange.receive(endpoint, receiveTimeoutMillis) ?: return@repeat
            when (LibomvPacketCodec.packetType(inbound.payload)) {
                SimulatorPacketType.START_PING_CHECK -> {
                    val pingId = LibomvPacketCodec.startPingId(inbound.payload)
                        ?: return WaitForPacketResult.Failed(SimulatorPresenceStatus.PING_REPLY_FAILED)
                    try {
                        packetExchange.send(
                            endpoint,
                            listOf(LibomvPacketCodec.completePingCheck(pingId, sequence.next())),
                        )
                    } catch (ex: Exception) {
                        return WaitForPacketResult.Failed(SimulatorPresenceStatus.PING_REPLY_FAILED)
                    }
                    replies += 1
                }
                wantedType -> return WaitForPacketResult.Found(replies)
                SimulatorPacketType.UNKNOWN,
                SimulatorPacketType.GROUP_NOTICES_LIST_REPLY,
                SimulatorPacketType.GROUP_NOTICE_REQUESTED,
                SimulatorPacketType.IMPROVED_INSTANT_MESSAGE,
                -> Unit
                else -> Unit
            }
        }
        return WaitForPacketResult.Failed(timeoutStatus)
    }

    private sealed interface WaitForPacketResult {
        data class Found(val pingReplies: Int) : WaitForPacketResult
        data class Failed(val status: SimulatorPresenceStatus) : WaitForPacketResult
    }

    private data class SimulatorCircuitKey(
        val agentId: String,
        val sessionId: String,
        val simulatorIp: String,
        val simulatorPort: Int,
        val circuitCode: Long,
    )

    private fun SimulatorCircuit.toKey(): SimulatorCircuitKey =
        SimulatorCircuitKey(
            agentId = agentId,
            sessionId = sessionId,
            simulatorIp = simulatorIp,
            simulatorPort = simulatorPort,
            circuitCode = circuitCode,
        )

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
        const val HANDSHAKE_RECEIVE_TIMEOUT_MILLIS: Int = 2_000
        const val HANDSHAKE_RECEIVE_ATTEMPTS: Int = 12
        const val MOVEMENT_RECEIVE_TIMEOUT_MILLIS: Int = 250
        const val MOVEMENT_RECEIVE_ATTEMPTS: Int = 12
    }
}
