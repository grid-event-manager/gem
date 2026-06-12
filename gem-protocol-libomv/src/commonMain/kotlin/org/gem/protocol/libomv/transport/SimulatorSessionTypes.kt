package org.gem.protocol.libomv.transport

internal sealed interface SimulatorLogoutResult

internal data object LoggedOut : SimulatorLogoutResult
internal data object ClosedWithoutReply : SimulatorLogoutResult
internal data class Failed(val redactedMessage: String) : SimulatorLogoutResult

internal data class SimulatorSessionHealth(
    val status: SimulatorSessionHealthStatus,
    val redactedMessage: String? = null,
)

internal enum class SimulatorSessionHealthStatus {
    INACTIVE,
    CONNECTING,
    PRESENT,
    STALE,
    FAILED,
    LOGGING_OUT,
    CLOSED,
}

internal data class SimulatorOutboundPacket(
    val payload: ByteArray,
    val reliableSequenceNumber: Long? = null,
)

internal sealed interface SimulatorInboundAction

internal data class AckReliable(val sequenceNumber: Long, val packet: ByteArray) : SimulatorInboundAction
internal data class ReplyToPing(val pingId: Int, val packet: ByteArray) : SimulatorInboundAction
internal data class RecordOutgoingAck(val sequenceNumber: Long) : SimulatorInboundAction
internal data class MatchedArchiveReply(
    val groupId: String,
    val entries: List<SimulatorNoticeArchiveEntry>,
) : SimulatorInboundAction

internal data object MatchedLogoutReply : SimulatorInboundAction
internal data class ObserveNoticeTraffic(val redactedSummary: String) : SimulatorInboundAction
internal data class MarkFailed(val redactedMessage: String) : SimulatorInboundAction
internal data object IgnoreInbound : SimulatorInboundAction

internal sealed interface SimulatorReliableSendDecision

internal data class Resend(val packet: SimulatorOutboundPacket) : SimulatorReliableSendDecision
internal data object AwaitAck : SimulatorReliableSendDecision
internal data class TimedOut(val redactedMessage: String) : SimulatorReliableSendDecision
