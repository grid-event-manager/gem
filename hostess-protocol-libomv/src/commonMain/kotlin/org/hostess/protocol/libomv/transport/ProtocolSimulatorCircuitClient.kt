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
    private val packetExchange: SimulatorPacketExchange,
    private val sequence: SimulatorPacketSequence = SimulatorPacketSequence(),
) {
    private var presentCircuit: SimulatorPresenceCache? = null
    private val pendingNoticeArchiveReplies = mutableMapOf<String, List<SimulatorNoticeArchiveEntry>>()

    fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult =
        sendAfterPresence(circuit) {
            LibomvPacketCodec.agentDataUpdateRequest(circuit, sequence.next())
        }

    fun sendNotice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorCircuitSendResult =
        sendReliableAfterPresence(circuit) {
            val packetSequence = sequence.next()
            ReliablePayload(
                sequenceNumber = packetSequence.toLong(),
                bytes = LibomvNoticePacketCodec.improvedInstantMessage(packet, packetSequence),
            )
        }

    fun requestGroupNoticeArchive(circuit: SimulatorCircuit, groupId: String): SimulatorNoticeArchiveResult {
        val canonicalGroupId = LibomvUuidCodec.canonicalOrNull(groupId)
        if (!circuit.isUsable() || canonicalGroupId == null) {
            return SimulatorNoticeArchiveResult.Failed(
                status = SimulatorNoticeArchiveStatus.REQUEST_INVALID,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
        when (val presence = ensurePresence(circuit)) {
            is SimulatorPresenceResult.Failed -> return SimulatorNoticeArchiveResult.Failed(
                status = presence.status.toArchiveStatus(),
                redactedMessage = REDACTED_SEND_FAILURE,
            )
            is SimulatorPresenceResult.Present -> Unit
        }
        pendingNoticeArchiveReplies.remove(canonicalGroupId)?.let { cached ->
            return SimulatorNoticeArchiveResult.Found(cached)
        }

        try {
            packetExchange.send(
                endpoint,
                listOf(LibomvPacketCodec.groupNoticesListRequest(circuit, canonicalGroupId, sequence.next())),
            )
        } catch (ex: Exception) {
            return SimulatorNoticeArchiveResult.Failed(
                status = SimulatorNoticeArchiveStatus.REQUEST_SEND_FAILED,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }

        return try {
            waitForArchiveReply(endpoint, canonicalGroupId)
        } catch (ex: Exception) {
            SimulatorNoticeArchiveResult.Failed(
                status = SimulatorNoticeArchiveStatus.REQUEST_SEND_FAILED,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }
    }

    fun ensurePresence(circuit: SimulatorCircuit): SimulatorPresenceResult {
        if (!circuit.isUsable()) {
            return SimulatorPresenceResult.Failed(
                status = SimulatorPresenceStatus.CIRCUIT_INVALID,
                redactedMessage = REDACTED_SEND_FAILURE,
            )
        }
        val circuitKey = circuit.toKey()
        val cachedPresence = presentCircuit
        if (cachedPresence?.circuitKey == circuitKey) {
            return SimulatorPresenceResult.Present(
                pingReplies = 0,
                cached = true,
                regionProtocolFlags = cachedPresence.regionProtocolFlags,
            )
        }
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)

        return try {
            sendPresencePacket(endpoint, SimulatorPresenceStatus.USE_CIRCUIT_CODE_FAILED) {
                LibomvPacketCodec.useCircuitCode(circuit, sequence.next())
            }?.let { return it }
            var movementSentBeforeHandshake = false
            val handshake = when (
                val result = waitForPacket(
                    endpoint = endpoint,
                    receiveTimeoutMillis = HANDSHAKE_RECEIVE_TIMEOUT_MILLIS,
                    maxAttempts = HANDSHAKE_RECEIVE_ATTEMPTS,
                    wantedType = SimulatorPacketType.REGION_HANDSHAKE,
                    timeoutStatus = SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
                )
            ) {
                is WaitForPacketResult.Failed -> {
                    if (!result.canTriggerLoginHandshake()) {
                        return result.toPresenceFailure()
                    }
                    sendPresencePacket(endpoint, SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED) {
                        LibomvPacketCodec.completeAgentMovement(circuit, sequence.next())
                    }?.let { return it }
                    movementSentBeforeHandshake = true
                    when (
                        val postMovement = waitForPacket(
                            endpoint = endpoint,
                            receiveTimeoutMillis = HANDSHAKE_RECEIVE_TIMEOUT_MILLIS,
                            maxAttempts = HANDSHAKE_RECEIVE_ATTEMPTS,
                            wantedType = SimulatorPacketType.REGION_HANDSHAKE,
                            timeoutStatus = SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
                            pingReplies = result.pingReplies,
                        )
                    ) {
                        is WaitForPacketResult.Failed -> return postMovement.toPresenceFailure()
                        is WaitForPacketResult.Found -> postMovement
                    }
                }
                is WaitForPacketResult.Found -> result
            }
            val handshakeInfo = LibomvPacketCodec.regionHandshakeInfo(handshake.payload)
                ?: return SimulatorPresenceResult.Failed(
                    status = SimulatorPresenceStatus.HANDSHAKE_MALFORMED,
                    redactedMessage = REDACTED_SEND_FAILURE,
                )
            sendPresencePacket(endpoint, SimulatorPresenceStatus.HANDSHAKE_REPLY_FAILED) {
                LibomvPacketCodec.regionHandshakeReply(circuit, sequence.next())
            }?.let { return it }
            if (!movementSentBeforeHandshake) {
                sendPresencePacket(endpoint, SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED) {
                    LibomvPacketCodec.completeAgentMovement(circuit, sequence.next())
                }?.let { return it }
            }
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
            presentCircuit = SimulatorPresenceCache(
                circuitKey = circuitKey,
                regionProtocolFlags = handshakeInfo.regionProtocolFlags,
            )
            SimulatorPresenceResult.Present(
                pingReplies = movement.pingReplies,
                cached = false,
                regionProtocolFlags = handshakeInfo.regionProtocolFlags,
            )
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

    private fun WaitForPacketResult.Failed.canTriggerLoginHandshake(): Boolean =
        status == SimulatorPresenceStatus.HANDSHAKE_TIMEOUT && observedPackets > 0

    private fun SimulatorPresenceStatus.toArchiveStatus(): SimulatorNoticeArchiveStatus =
        when (this) {
            SimulatorPresenceStatus.CIRCUIT_INVALID -> SimulatorNoticeArchiveStatus.REQUEST_INVALID
            SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
            SimulatorPresenceStatus.HANDSHAKE_MALFORMED,
            SimulatorPresenceStatus.MOVEMENT_TIMEOUT,
            -> SimulatorNoticeArchiveStatus.PRESENCE_PROOF_GAP
            SimulatorPresenceStatus.SEND_FAILED,
            SimulatorPresenceStatus.USE_CIRCUIT_CODE_FAILED,
            SimulatorPresenceStatus.PING_REPLY_FAILED,
            SimulatorPresenceStatus.HANDSHAKE_REPLY_FAILED,
            SimulatorPresenceStatus.COMPLETE_AGENT_MOVEMENT_FAILED,
            SimulatorPresenceStatus.AGENT_UPDATE_FAILED,
            -> SimulatorNoticeArchiveStatus.PRESENCE_TRANSPORT_GAP
        }

    private fun waitForArchiveReply(
        endpoint: SimulatorEndpoint,
        groupId: String,
    ): SimulatorNoticeArchiveResult {
        var sawMalformedReply = false
        var sawWrongGroupReply = false
        repeat(ARCHIVE_RECEIVE_ATTEMPTS) {
            val inbound = packetExchange.receive(endpoint, ARCHIVE_RECEIVE_TIMEOUT_MILLIS) ?: return@repeat
            if (!ackReliablePacket(endpoint, inbound.payload)) {
                return SimulatorNoticeArchiveResult.Failed(
                    status = SimulatorNoticeArchiveStatus.REQUEST_SEND_FAILED,
                    redactedMessage = REDACTED_SEND_FAILURE,
                )
            }
            when (LibomvPacketCodec.packetType(inbound.payload)) {
                SimulatorPacketType.START_PING_CHECK -> {
                    if (!answerSimulatorPing(endpoint, inbound.payload)) {
                        return SimulatorNoticeArchiveResult.Failed(
                            status = SimulatorNoticeArchiveStatus.REQUEST_SEND_FAILED,
                            redactedMessage = REDACTED_SEND_FAILURE,
                        )
                    }
                }
                SimulatorPacketType.GROUP_NOTICES_LIST_REPLY -> {
                    val reply = LibomvPacketCodec.groupNoticesListReply(inbound.payload)
                    if (reply == null) {
                        sawMalformedReply = true
                    } else if (reply.groupId == groupId) {
                        return SimulatorNoticeArchiveResult.Found(reply.entries)
                    } else {
                        sawWrongGroupReply = true
                        pendingNoticeArchiveReplies[reply.groupId] = reply.entries
                    }
                }
                else -> Unit
            }
        }
        val status = when {
            sawWrongGroupReply -> SimulatorNoticeArchiveStatus.WRONG_GROUP_REPLY
            sawMalformedReply -> SimulatorNoticeArchiveStatus.REPLY_MALFORMED
            else -> SimulatorNoticeArchiveStatus.REPLY_TIMEOUT
        }
        return SimulatorNoticeArchiveResult.Failed(status, REDACTED_SEND_FAILURE)
    }

    private fun sendAfterPresence(
        circuit: SimulatorCircuit,
        payload: () -> ByteArray,
    ): SimulatorCircuitSendResult {
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
        return when (val presence = ensurePresence(circuit)) {
            is SimulatorPresenceResult.Failed -> SimulatorCircuitSendResult.Failed(presence.redactedMessage)
            is SimulatorPresenceResult.Present -> try {
                packetExchange.send(endpoint, listOf(payload()))
                SimulatorCircuitSendResult.Sent()
            } catch (ex: IllegalArgumentException) {
                SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
            } catch (ex: Exception) {
                SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
            }
        }
    }

    private fun sendReliableAfterPresence(
        circuit: SimulatorCircuit,
        payload: () -> ReliablePayload,
    ): SimulatorCircuitSendResult {
        val endpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
        return when (val presence = ensurePresence(circuit)) {
            is SimulatorPresenceResult.Failed -> SimulatorCircuitSendResult.Failed(presence.redactedMessage)
            is SimulatorPresenceResult.Present -> {
                val reliable = try {
                    payload()
                } catch (ex: IllegalArgumentException) {
                    return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
                }
                val observations = SimulatorNoticeSendObservationCollector()
                repeat(RELIABLE_SEND_ATTEMPTS) { attempt ->
                    try {
                        packetExchange.send(endpoint, listOf(reliable.bytesForAttempt(attempt)))
                    } catch (ex: Exception) {
                        return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
                    }
                    when (waitForOutgoingAck(circuit, endpoint, reliable.sequenceNumber, observations)) {
                        OutgoingAckResult.ACKED -> {
                            if (drainNoticeSendObservations(endpoint, observations) == OutgoingAckResult.FAILED) {
                                return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
                            }
                            return SimulatorCircuitSendResult.Sent(observations.redactedSummary())
                        }
                        OutgoingAckResult.FAILED -> return SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
                        OutgoingAckResult.TIMEOUT -> Unit
                    }
                }
                SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
            }
        }
    }

    private fun waitForOutgoingAck(
        circuit: SimulatorCircuit,
        endpoint: SimulatorEndpoint,
        sequenceNumber: Long,
        observations: SimulatorNoticeSendObservationCollector,
    ): OutgoingAckResult {
        var keepAliveSent = false
        var observedPackets = 0
        var receiveTimeouts = 0
        while (
            observedPackets < NOTICE_ACK_RECEIVE_PACKET_LIMIT &&
            receiveTimeouts < NOTICE_ACK_RECEIVE_TIMEOUT_LIMIT
        ) {
            val inbound = packetExchange.receive(endpoint, NOTICE_ACK_RECEIVE_TIMEOUT_MILLIS)
            if (inbound == null) {
                receiveTimeouts += 1
                if (!keepAliveSent && !sendAvatarKeepAlive(endpoint, circuit)) {
                    return OutgoingAckResult.FAILED
                }
                keepAliveSent = true
                continue
            }
            observedPackets += 1
            if (!ackReliablePacket(endpoint, inbound.payload)) {
                return OutgoingAckResult.FAILED
            }
            observations.record(inbound.payload)
            val acked = LibomvPacketCodec.packetAckSequences(inbound.payload)
            if (acked?.contains(sequenceNumber) == true) {
                return OutgoingAckResult.ACKED
            }
            when (LibomvPacketCodec.packetType(inbound.payload)) {
                SimulatorPacketType.START_PING_CHECK -> {
                    if (!answerSimulatorPing(endpoint, inbound.payload)) {
                        return OutgoingAckResult.FAILED
                    }
                }
                else -> Unit
            }
        }
        return OutgoingAckResult.TIMEOUT
    }

    private fun drainNoticeSendObservations(
        endpoint: SimulatorEndpoint,
        observations: SimulatorNoticeSendObservationCollector,
    ): OutgoingAckResult {
        var observedPackets = 0
        var receiveTimeouts = 0
        while (
            observedPackets < NOTICE_POST_ACK_RECEIVE_PACKET_LIMIT &&
            receiveTimeouts < NOTICE_POST_ACK_RECEIVE_TIMEOUT_LIMIT
        ) {
            val inbound = packetExchange.receive(endpoint, NOTICE_POST_ACK_RECEIVE_TIMEOUT_MILLIS)
            if (inbound == null) {
                receiveTimeouts += 1
                continue
            }
            observedPackets += 1
            receiveTimeouts = 0
            if (!ackReliablePacket(endpoint, inbound.payload)) {
                return OutgoingAckResult.FAILED
            }
            observations.record(inbound.payload)
            when (LibomvPacketCodec.packetType(inbound.payload)) {
                SimulatorPacketType.START_PING_CHECK -> {
                    if (!answerSimulatorPing(endpoint, inbound.payload)) {
                        return OutgoingAckResult.FAILED
                    }
                }
                else -> Unit
            }
        }
        return OutgoingAckResult.ACKED
    }

    private fun sendAvatarKeepAlive(endpoint: SimulatorEndpoint, circuit: SimulatorCircuit): Boolean =
        try {
            packetExchange.send(endpoint, listOf(LibomvPacketCodec.agentUpdate(circuit, sequence.next())))
            true
        } catch (ex: Exception) {
            false
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
        var observedPackets = 0
        repeat(maxAttempts) {
            val inbound = packetExchange.receive(endpoint, receiveTimeoutMillis) ?: return@repeat
            observedPackets += 1
            if (!ackReliablePacket(endpoint, inbound.payload)) {
                return WaitForPacketResult.Failed(
                    status = SimulatorPresenceStatus.SEND_FAILED,
                    pingReplies = replies,
                    observedPackets = observedPackets,
                )
            }
            val packetType = LibomvPacketCodec.packetType(inbound.payload)
            when (packetType) {
                SimulatorPacketType.START_PING_CHECK -> {
                    if (!answerSimulatorPing(endpoint, inbound.payload)) {
                        return WaitForPacketResult.Failed(
                            status = SimulatorPresenceStatus.PING_REPLY_FAILED,
                            pingReplies = replies,
                            observedPackets = observedPackets,
                        )
                    }
                    replies += 1
                }
                wantedType -> return WaitForPacketResult.Found(replies, inbound.payload)
                SimulatorPacketType.UNKNOWN,
                SimulatorPacketType.GROUP_NOTICES_LIST_REPLY,
                SimulatorPacketType.GROUP_NOTICE_REQUESTED,
                SimulatorPacketType.IMPROVED_INSTANT_MESSAGE,
                -> Unit
                else -> Unit
            }
        }
        return WaitForPacketResult.Failed(
            status = timeoutStatus,
            pingReplies = replies,
            observedPackets = observedPackets,
        )
    }

    private fun ackReliablePacket(endpoint: SimulatorEndpoint, payload: ByteArray): Boolean {
        val sequenceNumber = LibomvPacketCodec.reliableSequenceNumber(payload) ?: return true
        return try {
            packetExchange.send(endpoint, listOf(LibomvPacketCodec.packetAck(sequenceNumber)))
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun answerSimulatorPing(endpoint: SimulatorEndpoint, payload: ByteArray): Boolean {
        val pingId = LibomvPacketCodec.startPingId(payload) ?: return false
        return try {
            packetExchange.send(
                endpoint,
                listOf(LibomvPacketCodec.completePingCheck(pingId, sequence.next())),
            )
            true
        } catch (ex: Exception) {
            false
        }
    }

    private sealed interface WaitForPacketResult {
        data class Found(
            val pingReplies: Int,
            val payload: ByteArray,
        ) : WaitForPacketResult

        data class Failed(
            val status: SimulatorPresenceStatus,
            val pingReplies: Int,
            val observedPackets: Int,
        ) : WaitForPacketResult
    }

    private data class SimulatorCircuitKey(
        val agentId: String,
        val sessionId: String,
        val simulatorIp: String,
        val simulatorPort: Int,
        val circuitCode: Long,
    )

    private data class SimulatorPresenceCache(
        val circuitKey: SimulatorCircuitKey,
        val regionProtocolFlags: RegionProtocolFlags,
    )

    private data class ReliablePayload(
        val sequenceNumber: Long,
        val bytes: ByteArray,
    ) {
        fun bytesForAttempt(attempt: Int): ByteArray =
            if (attempt == 0) bytes else LibomvPacketCodec.asResent(bytes)
    }

    private enum class OutgoingAckResult {
        ACKED,
        TIMEOUT,
        FAILED,
    }

    private class SimulatorNoticeSendObservationCollector {
        private val packets = mutableListOf<SimulatorPacketObservation>()
        private val instantMessages = mutableListOf<SimulatorInstantMessageObservation>()
        private val alertMessages = mutableListOf<SimulatorAlertMessageObservation>()

        fun record(payload: ByteArray) {
            val type = LibomvPacketCodec.packetType(payload)
            packets += SimulatorPacketObservation(
                type = type,
                packetLabel = LibomvPacketCodec.decodedPacketLabel(payload),
                knownName = LibomvPacketCodec.decodedPacketKnownName(payload),
            )
            when (type) {
                SimulatorPacketType.IMPROVED_INSTANT_MESSAGE ->
                    LibomvPacketCodec.improvedInstantMessageObservation(payload)?.let(instantMessages::add)
                SimulatorPacketType.ALERT_MESSAGE ->
                    LibomvPacketCodec.alertMessageObservation(payload)?.let(alertMessages::add)
                else -> Unit
            }
        }

        fun redactedSummary(): String = buildList {
            add("transportAck=passed")
            add("observedPackets=${packets.size}")
            add("packetTypes=${packetTypeSummary()}")
            add("instantMessages=${instantMessages.size}")
            add("alertMessages=${alertMessages.size}")
            instantMessages.take(MAX_REPORTED_INSTANT_MESSAGES).forEachIndexed { index, message ->
                add("im[$index]={${message.summary}}")
            }
            alertMessages.take(MAX_REPORTED_ALERT_MESSAGES).forEachIndexed { index, message ->
                add("alert[$index]={${message.summary}}")
            }
        }.joinToString("; ")

        private fun packetTypeSummary(): String =
            if (packets.isEmpty()) {
                "none"
            } else {
                packets.groupingBy(SimulatorPacketObservation::reportName)
                    .eachCount()
                    .entries
                    .joinToString(",") { "${it.key}:${it.value}" }
            }

        private data class SimulatorPacketObservation(
            val type: SimulatorPacketType,
            val packetLabel: String?,
            val knownName: String?,
        ) {
            fun reportName(): String =
                when {
                    type == SimulatorPacketType.UNKNOWN && knownName != null -> knownName
                    type == SimulatorPacketType.UNKNOWN && packetLabel != null -> "unknown_$packetLabel"
                    else -> type.name.lowercase()
                }
        }

        private companion object {
            const val MAX_REPORTED_INSTANT_MESSAGES: Int = 3
            const val MAX_REPORTED_ALERT_MESSAGES: Int = 3
        }
    }

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
        const val ARCHIVE_RECEIVE_TIMEOUT_MILLIS: Int = 250
        const val ARCHIVE_RECEIVE_ATTEMPTS: Int = 12
        const val NOTICE_ACK_RECEIVE_TIMEOUT_MILLIS: Int = 250
        const val NOTICE_ACK_RECEIVE_PACKET_LIMIT: Int = 128
        const val NOTICE_ACK_RECEIVE_TIMEOUT_LIMIT: Int = 8
        const val NOTICE_POST_ACK_RECEIVE_TIMEOUT_MILLIS: Int = 250
        const val NOTICE_POST_ACK_RECEIVE_PACKET_LIMIT: Int = 20
        const val NOTICE_POST_ACK_RECEIVE_TIMEOUT_LIMIT: Int = 4
        const val RELIABLE_SEND_ATTEMPTS: Int = 3
    }
}
