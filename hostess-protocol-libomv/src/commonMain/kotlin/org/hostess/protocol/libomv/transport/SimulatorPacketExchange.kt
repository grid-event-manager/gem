package org.hostess.protocol.libomv.transport

import org.hostess.core.services.SafeDiagnosticRedaction

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
    ALERT_MESSAGE,
    PACKET_ACK,
    UNKNOWN,
}

internal data class SimulatorInstantMessageObservation(
    val dialog: Int,
    val offline: Int,
    val fromGroup: Boolean,
    val message: String,
    val binaryBucketBytes: Int,
) {
    val summary: String
        get() = buildList {
            add("dialog=$dialog")
            add("offline=$offline")
            add("fromGroup=$fromGroup")
            add("messageLength=${message.length}")
            if (message.isNotBlank()) {
                add("messagePreview=${SafeDiagnosticRedaction.excerpt(message, MAX_MESSAGE_PREVIEW)}")
            }
            add("binaryBucketBytes=$binaryBucketBytes")
        }.joinToString(",")

    private companion object {
        const val MAX_MESSAGE_PREVIEW: Int = 96
    }
}

internal data class SimulatorAlertMessageObservation(
    val message: String,
    val alertInfoCount: Int,
) {
    val summary: String
        get() = buildList {
            add("messageLength=${message.length}")
            if (message.isNotBlank()) {
                add("messagePreview=${SafeDiagnosticRedaction.excerpt(message, MAX_MESSAGE_PREVIEW)}")
            }
            add("alertInfoCount=$alertInfoCount")
        }.joinToString(",")

    private companion object {
        const val MAX_MESSAGE_PREVIEW: Int = 160
    }
}

internal sealed interface SimulatorPresenceResult {
    data class Present(
        val pingReplies: Int,
        val cached: Boolean,
        val regionProtocolFlags: RegionProtocolFlags = RegionProtocolFlags.unknown(),
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

internal sealed interface SimulatorNoticeArchiveResult {
    data class Found(
        val entries: List<SimulatorNoticeArchiveEntry>,
    ) : SimulatorNoticeArchiveResult

    data class Failed(
        val status: SimulatorNoticeArchiveStatus,
        val redactedMessage: String,
    ) : SimulatorNoticeArchiveResult
}

internal data class SimulatorNoticeArchiveEntry(
    val noticeId: String,
    val timestamp: Long,
    val fromName: String,
    val subject: String,
    val hasAttachment: Boolean,
    val assetType: Int,
)

internal data class SimulatorNoticeArchiveReply(
    val groupId: String,
    val entries: List<SimulatorNoticeArchiveEntry>,
)

internal enum class SimulatorNoticeArchiveStatus {
    REQUEST_INVALID,
    PRESENCE_TRANSPORT_GAP,
    PRESENCE_PROOF_GAP,
    REQUEST_SEND_FAILED,
    REPLY_TIMEOUT,
    REPLY_MALFORMED,
    WRONG_GROUP_REPLY,
}
