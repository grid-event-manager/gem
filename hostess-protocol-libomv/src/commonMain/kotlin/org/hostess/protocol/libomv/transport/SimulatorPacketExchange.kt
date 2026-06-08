package org.hostess.protocol.libomv.transport

internal data class SimulatorEndpoint(
    val host: String,
    val port: Int,
)

internal data class SimulatorInboundPacket(
    val endpoint: SimulatorEndpoint,
    val payload: ByteArray,
)

internal interface SimulatorPacketExchange {
    fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>)
    fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket?
}

internal class SimulatorPacketExchangeException : RuntimeException("protocol simulator exchange failed")

internal enum class SimulatorPacketType {
    REGION_HANDSHAKE,
    START_PING_CHECK,
    AGENT_MOVEMENT_COMPLETE,
    GROUP_NOTICES_LIST_REPLY,
    GROUP_NOTICE_REQUESTED,
    IMPROVED_INSTANT_MESSAGE,
    UNKNOWN,
}

internal sealed interface SimulatorPresenceResult {
    data class Present(
        val pingReplies: Int,
        val cached: Boolean,
    ) : SimulatorPresenceResult

    data class Failed(
        val status: SimulatorPresenceStatus,
        val redactedMessage: String,
    ) : SimulatorPresenceResult
}

internal enum class SimulatorPresenceStatus {
    CIRCUIT_INVALID,
    SEND_FAILED,
    USE_CIRCUIT_CODE_FAILED,
    HANDSHAKE_TIMEOUT,
    HANDSHAKE_MALFORMED,
    PING_REPLY_FAILED,
    HANDSHAKE_REPLY_FAILED,
    COMPLETE_AGENT_MOVEMENT_FAILED,
    MOVEMENT_TIMEOUT,
    AGENT_UPDATE_FAILED,
}
