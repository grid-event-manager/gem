package org.gem.protocol.libomv.transport

import org.gem.protocol.libomv.mapping.LibomvNoticePacket
import org.gem.protocol.libomv.mapping.LibomvUuidCodec

internal class SimulatorSessionProtocol(
    private val sequence: SimulatorPacketSequence = SimulatorPacketSequence(),
) {
    private val pendingReliableSends = mutableMapOf<Long, PendingReliablePacket>()
    private val requestedArchiveGroups = mutableSetOf<String>()
    private var health = SimulatorSessionHealth(SimulatorSessionHealthStatus.INACTIVE)

    fun useCircuitCode(circuit: SimulatorCircuit): SimulatorOutboundPacket {
        health = SimulatorSessionHealth(SimulatorSessionHealthStatus.CONNECTING)
        return outbound(LibomvPacketCodec.useCircuitCode(circuit, sequence.next()))
    }

    fun regionHandshakeReply(circuit: SimulatorCircuit): SimulatorOutboundPacket =
        reliableOutbound { LibomvPacketCodec.regionHandshakeReply(circuit, it) }

    fun completeAgentMovement(circuit: SimulatorCircuit): SimulatorOutboundPacket =
        outbound(LibomvPacketCodec.completeAgentMovement(circuit, sequence.next()))

    fun initialAgentUpdate(circuit: SimulatorCircuit): SimulatorOutboundPacket {
        health = SimulatorSessionHealth(SimulatorSessionHealthStatus.PRESENT)
        return reliableOutbound { LibomvPacketCodec.agentUpdate(circuit, it, reliable = true) }
    }

    fun heartbeat(circuit: SimulatorCircuit): SimulatorOutboundPacket {
        if (health.status != SimulatorSessionHealthStatus.LOGGING_OUT) {
            health = SimulatorSessionHealth(SimulatorSessionHealthStatus.PRESENT)
        }
        return outbound(LibomvPacketCodec.agentUpdate(circuit, sequence.next(), reliable = false))
    }

    fun currentGroupsRequest(circuit: SimulatorCircuit): SimulatorOutboundPacket =
        outbound(LibomvPacketCodec.agentDataUpdateRequest(circuit, sequence.next()))

    fun notice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorOutboundPacket =
        reliableOutbound { LibomvNoticePacketCodec.improvedInstantMessage(packet, it) }

    fun noticeArchiveRequest(circuit: SimulatorCircuit, groupId: String): SimulatorOutboundPacket {
        LibomvUuidCodec.canonicalOrNull(groupId)?.let(requestedArchiveGroups::add)
        return reliableOutbound { LibomvPacketCodec.groupNoticesListRequest(circuit, groupId, it) }
    }

    fun logoutRequest(circuit: SimulatorCircuit): SimulatorOutboundPacket {
        health = SimulatorSessionHealth(SimulatorSessionHealthStatus.LOGGING_OUT)
        return reliableOutbound { LibomvPacketCodec.logoutRequest(circuit, it) }
    }

    fun closeCircuit(): SimulatorOutboundPacket {
        health = SimulatorSessionHealth(SimulatorSessionHealthStatus.CLOSED)
        return outbound(LibomvPacketCodec.closeCircuit(sequence.next()))
    }

    fun onInbound(circuit: SimulatorCircuit, payload: ByteArray): List<SimulatorInboundAction> {
        val actions = mutableListOf<SimulatorInboundAction>()
        LibomvPacketCodec.reliableSequenceNumber(payload)?.let { sequenceNumber ->
            actions += AckReliable(
                sequenceNumber = sequenceNumber,
                packet = LibomvPacketCodec.packetAck(sequenceNumber),
            )
        }
        LibomvPacketCodec.packetAckSequences(payload).orEmpty().forEach { sequenceNumber ->
            if (pendingReliableSends.remove(sequenceNumber) != null) {
                actions += RecordOutgoingAck(sequenceNumber)
            }
        }
        when (LibomvPacketCodec.packetType(payload)) {
            SimulatorPacketType.START_PING_CHECK -> actions += pingAction(payload)
            SimulatorPacketType.GROUP_NOTICES_LIST_REPLY -> {
                val reply = LibomvPacketCodec.groupNoticesListReply(payload)
                when {
                    reply == null -> actions += MarkFailed(ARCHIVE_REPLY_MALFORMED)
                    requestedArchiveGroups.remove(reply.groupId) -> actions += MatchedArchiveReply(
                        groupId = reply.groupId,
                        entries = reply.entries,
                    )
                }
            }
            SimulatorPacketType.IMPROVED_INSTANT_MESSAGE -> LibomvPacketCodec.improvedInstantMessageObservation(payload)
                ?.let { actions += ObserveNoticeTraffic(it.summary) }
            SimulatorPacketType.ALERT_MESSAGE -> LibomvPacketCodec.alertMessageObservation(payload)
                ?.let { actions += ObserveNoticeTraffic(it.summary) }
            SimulatorPacketType.LOGOUT_REPLY -> if (LibomvPacketCodec.logoutReplyMatches(payload, circuit)) {
                health = SimulatorSessionHealth(SimulatorSessionHealthStatus.CLOSED)
                actions += MatchedLogoutReply
            }
            else -> Unit
        }
        return actions.ifEmpty { listOf(IgnoreInbound) }
    }

    fun onReliableSendTimeout(sequenceNumber: Long): SimulatorReliableSendDecision {
        val pending = pendingReliableSends[sequenceNumber] ?: return AwaitAck
        return if (pending.attempt >= MAX_RELIABLE_SEND_ATTEMPTS) {
            pendingReliableSends.remove(sequenceNumber)
            health = SimulatorSessionHealth(SimulatorSessionHealthStatus.FAILED, RELIABLE_ACK_TIMEOUT)
            TimedOut(RELIABLE_ACK_TIMEOUT)
        } else {
            val resent = LibomvPacketCodec.asResent(pending.packet.payload)
            pendingReliableSends[sequenceNumber] = pending.copy(
                packet = pending.packet.copy(payload = resent),
                attempt = pending.attempt + 1,
            )
            Resend(
                SimulatorOutboundPacket(payload = resent, reliableSequenceNumber = sequenceNumber),
            )
        }
    }

    fun health(): SimulatorSessionHealth = health

    private fun pingAction(payload: ByteArray): SimulatorInboundAction =
        LibomvPacketCodec.startPingId(payload)?.let { pingId ->
            ReplyToPing(
                pingId = pingId,
                packet = LibomvPacketCodec.completePingCheck(pingId, sequence.next()),
            )
        } ?: MarkFailed(PING_MALFORMED)

    private fun reliableOutbound(payload: (Int) -> ByteArray): SimulatorOutboundPacket {
        val packetSequence = sequence.next()
        return outbound(payload(packetSequence), packetSequence.toLong())
    }

    private fun outbound(payload: ByteArray, reliableSequenceNumber: Long? = null): SimulatorOutboundPacket {
        val packet = SimulatorOutboundPacket(payload, reliableSequenceNumber)
        reliableSequenceNumber?.let { sequenceNumber ->
            pendingReliableSends[sequenceNumber] = PendingReliablePacket(packet, attempt = 1)
        }
        return packet
    }

    private data class PendingReliablePacket(
        val packet: SimulatorOutboundPacket,
        val attempt: Int,
    )

    private companion object {
        const val MAX_RELIABLE_SEND_ATTEMPTS = 3
        const val RELIABLE_ACK_TIMEOUT = "reliable simulator send ack timeout after 3 attempts"
        const val PING_MALFORMED = "simulator ping malformed"
        const val ARCHIVE_REPLY_MALFORMED = "group notice archive reply malformed"
    }
}
