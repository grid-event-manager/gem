package org.gem.protocol.libomv.transport

import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.gem.protocol.libomv.mapping.LibomvNoticePacket

internal class ThreadedSimulatorSessionGateway(
    private val packetExchangeFactory: SimulatorPacketExchangeFactory,
) : SimulatorSessionGateway, AutoCloseable {
    private val workerLock = Any()
    private var worker: GatewayWorker? = null

    override fun ensurePresence(circuit: SimulatorCircuit): SimulatorPresenceResult =
        submit<SimulatorPresenceResult>(
            fallback = SimulatorPresenceResult.Failed(
                status = SimulatorPresenceStatus.SEND_FAILED,
                redactedMessage = REDACTED_SEND_FAILURE,
            ),
        ) { EnsurePresenceCommand(circuit, it) }

    override fun sendCurrentGroupsRequest(circuit: SimulatorCircuit): SimulatorCircuitSendResult =
        submit<SimulatorCircuitSendResult>(SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)) {
            SendCurrentGroupsCommand(circuit, it)
        }

    override fun sendNotice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorCircuitSendResult =
        submit<SimulatorCircuitSendResult>(SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)) {
            SendNoticeCommand(circuit, packet, it)
        }

    override fun requestGroupNoticeArchive(circuit: SimulatorCircuit, groupId: String): SimulatorNoticeArchiveResult =
        submit<SimulatorNoticeArchiveResult>(
            fallback = SimulatorNoticeArchiveResult.Failed(
                status = SimulatorNoticeArchiveStatus.REQUEST_SEND_FAILED,
                redactedMessage = REDACTED_SEND_FAILURE,
            ),
        ) { RequestArchiveCommand(circuit, groupId, it) }

    override fun logout(circuit: SimulatorCircuit): SimulatorLogoutResult =
        submit<SimulatorLogoutResult>(Failed(REDACTED_SEND_FAILURE)) { LogoutCommand(circuit, it) }

    override fun health(circuit: SimulatorCircuit): SimulatorSessionHealth =
        synchronized(workerLock) {
            worker?.health(circuit)
        } ?: SimulatorSessionHealth(SimulatorSessionHealthStatus.INACTIVE)

    override fun close() {
        synchronized(workerLock) {
            worker.also { worker = null }
        }?.stopWorker()
    }

    private fun <T> submit(fallback: T, command: (GatewayReply<T>) -> GatewayCommand): T =
        try {
            val reply = GatewayReply<T>()
            worker().submit(command(reply))
            reply.await()
        } catch (ex: Exception) {
            fallback
        }

    private fun worker(): GatewayWorker =
        synchronized(workerLock) {
            worker?.takeIf(GatewayWorker::isRunning) ?: GatewayWorker(packetExchangeFactory).also {
                worker = it
                it.start()
            }
        }

    private sealed interface GatewayCommand

    private data class EnsurePresenceCommand(
        val circuit: SimulatorCircuit,
        val reply: GatewayReply<SimulatorPresenceResult>,
    ) : GatewayCommand

    private data class SendCurrentGroupsCommand(
        val circuit: SimulatorCircuit,
        val reply: GatewayReply<SimulatorCircuitSendResult>,
    ) : GatewayCommand

    private data class SendNoticeCommand(
        val circuit: SimulatorCircuit,
        val packet: LibomvNoticePacket,
        val reply: GatewayReply<SimulatorCircuitSendResult>,
    ) : GatewayCommand

    private data class RequestArchiveCommand(
        val circuit: SimulatorCircuit,
        val groupId: String,
        val reply: GatewayReply<SimulatorNoticeArchiveResult>,
    ) : GatewayCommand

    private data class LogoutCommand(
        val circuit: SimulatorCircuit,
        val reply: GatewayReply<SimulatorLogoutResult>,
    ) : GatewayCommand

    private data object StopWorkerCommand : GatewayCommand

    private data class PendingReliableSend(
        val sequenceNumber: Long,
        val packet: SimulatorOutboundPacket,
        val attempt: Int,
    )

    private class GatewayReply<T> {
        private val latch = CountDownLatch(1)
        private var value: T? = null

        fun complete(result: T) {
            value = result
            latch.countDown()
        }

        fun await(): T {
            if (!latch.await(COMMAND_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                throw SimulatorPacketExchangeException()
            }
            return value ?: throw SimulatorPacketExchangeException()
        }
    }

    private class GatewayWorker(
        private val packetExchangeFactory: SimulatorPacketExchangeFactory,
    ) : Runnable {
        private val running = AtomicBoolean(true)
        private val commands = LinkedBlockingQueue<GatewayCommand>()
        private val thread = Thread(this, WORKER_THREAD_NAME).apply { isDaemon = true }
        private var exchange: SimulatorPacketExchange? = null
        @Volatile
        private var activeCircuit: SimulatorCircuit? = null
        private var activeEndpoint: SimulatorEndpoint? = null
        private var protocol = SimulatorSessionProtocol()
        @Volatile
        private var latestHealth = SimulatorSessionHealth(SimulatorSessionHealthStatus.INACTIVE)
        private var cachedPresence: CachedPresence? = null
        private var nextHeartbeatAt = 0L
        private var pingReplies = 0
        private var archiveMalformed = false
        private val archiveReplies = mutableMapOf<String, List<SimulatorNoticeArchiveEntry>>()

        fun start() {
            thread.start()
        }

        fun isRunning(): Boolean = running.get()

        fun submit(command: GatewayCommand) {
            if (!commands.offer(command)) {
                throw SimulatorPacketExchangeException()
            }
        }

        fun stopWorker() {
            if (running.get()) {
                submit(StopWorkerCommand)
                thread.join(COMMAND_TIMEOUT_MILLIS)
            }
        }

        fun health(circuit: SimulatorCircuit): SimulatorSessionHealth =
            if (activeCircuit == circuit) {
                latestHealth
            } else {
                SimulatorSessionHealth(SimulatorSessionHealthStatus.INACTIVE)
            }

        override fun run() {
            while (running.get()) {
                commands.poll(RECEIVE_POLL_MILLIS.toLong(), TimeUnit.MILLISECONDS)?.let(::handleCommand)
                serviceInbound(RECEIVE_POLL_MILLIS, null)
                sendHeartbeatIfDue()
            }
            closeExchange()
        }

        private fun handleCommand(command: GatewayCommand) {
            when (command) {
                is EnsurePresenceCommand -> command.reply.complete(ensurePresence(command.circuit))
                is SendCurrentGroupsCommand -> command.reply.complete(sendCurrentGroups(command.circuit))
                is SendNoticeCommand -> command.reply.complete(sendNotice(command.circuit, command.packet))
                is RequestArchiveCommand -> command.reply.complete(requestArchive(command.circuit, command.groupId))
                is LogoutCommand -> command.reply.complete(logout(command.circuit))
                StopWorkerCommand -> running.set(false)
            }
        }

        private fun ensurePresence(circuit: SimulatorCircuit): SimulatorPresenceResult {
            cachedPresence?.takeIf { it.circuit == circuit && protocol.health().status == SimulatorSessionHealthStatus.PRESENT }
                ?.let { cached ->
                    return SimulatorPresenceResult.Present(
                        pingReplies = pingReplies,
                        cached = true,
                        regionName = cached.regionName,
                        regionProtocolFlags = cached.regionProtocolFlags,
                        heartbeatActive = true,
                        sessionHealth = protocol.health(),
                    )
                }

            resetForCircuit(circuit)
            return try {
                send(protocol.useCircuitCode(circuit))
                var movementSentBeforeHandshake = false
                var handshake = receiveUntil(
                    wantedType = SimulatorPacketType.REGION_HANDSHAKE,
                    receiveTimeoutMillis = HANDSHAKE_RECEIVE_TIMEOUT_MILLIS,
                    maxAttempts = HANDSHAKE_RECEIVE_ATTEMPTS,
                )
                if (handshake is ReceiveSearchResult.Failed && handshake.observedPackets > 0) {
                    send(protocol.completeAgentMovement(circuit))
                    movementSentBeforeHandshake = true
                    handshake = receiveUntil(
                        wantedType = SimulatorPacketType.REGION_HANDSHAKE,
                        receiveTimeoutMillis = HANDSHAKE_RECEIVE_TIMEOUT_MILLIS,
                        maxAttempts = HANDSHAKE_RECEIVE_ATTEMPTS,
                    )
                }
                val handshakePayload = when (handshake) {
                    is ReceiveSearchResult.Found -> handshake.payload
                    is ReceiveSearchResult.Failed -> return SimulatorPresenceResult.Failed(
                        status = SimulatorPresenceStatus.HANDSHAKE_TIMEOUT,
                        redactedMessage = REDACTED_SEND_FAILURE,
                    )
                }
                val handshakeInfo = LibomvPacketCodec.regionHandshakeInfo(handshakePayload)
                    ?: return SimulatorPresenceResult.Failed(
                        status = SimulatorPresenceStatus.HANDSHAKE_MALFORMED,
                        redactedMessage = REDACTED_SEND_FAILURE,
                    )
                send(protocol.regionHandshakeReply(circuit))
                if (!movementSentBeforeHandshake) {
                    send(protocol.completeAgentMovement(circuit))
                }
                when (
                    receiveUntil(
                        wantedType = SimulatorPacketType.AGENT_MOVEMENT_COMPLETE,
                        receiveTimeoutMillis = MOVEMENT_RECEIVE_TIMEOUT_MILLIS,
                        maxAttempts = MOVEMENT_RECEIVE_ATTEMPTS,
                    )
                ) {
                    is ReceiveSearchResult.Found -> Unit
                    is ReceiveSearchResult.Failed -> return SimulatorPresenceResult.Failed(
                        status = SimulatorPresenceStatus.MOVEMENT_TIMEOUT,
                        redactedMessage = REDACTED_SEND_FAILURE,
                    )
                }
                send(protocol.initialAgentUpdate(circuit))
                cachedPresence = CachedPresence(
                    circuit = circuit,
                    regionName = handshakeInfo.regionName,
                    regionProtocolFlags = handshakeInfo.regionProtocolFlags,
                )
                nextHeartbeatAt = 0L
                SimulatorPresenceResult.Present(
                    pingReplies = pingReplies,
                    cached = false,
                    regionName = handshakeInfo.regionName,
                    regionProtocolFlags = handshakeInfo.regionProtocolFlags,
                    heartbeatActive = true,
                    sessionHealth = protocol.health(),
                )
            } catch (ex: IllegalArgumentException) {
                SimulatorPresenceResult.Failed(SimulatorPresenceStatus.HANDSHAKE_MALFORMED, REDACTED_SEND_FAILURE)
            } catch (ex: Exception) {
                SimulatorPresenceResult.Failed(SimulatorPresenceStatus.SEND_FAILED, REDACTED_SEND_FAILURE)
            }
        }

        private fun sendCurrentGroups(circuit: SimulatorCircuit): SimulatorCircuitSendResult =
            when (val presence = ensurePresence(circuit)) {
                is SimulatorPresenceResult.Failed -> SimulatorCircuitSendResult.Failed(presence.redactedMessage)
                is SimulatorPresenceResult.Present -> try {
                    send(protocol.currentGroupsRequest(circuit))
                    SimulatorCircuitSendResult.Sent()
                } catch (ex: Exception) {
                    SimulatorCircuitSendResult.Failed(REDACTED_SEND_FAILURE)
                }
            }

        private fun sendNotice(circuit: SimulatorCircuit, packet: LibomvNoticePacket): SimulatorCircuitSendResult =
            when (val presence = ensurePresence(circuit)) {
                is SimulatorPresenceResult.Failed -> SimulatorCircuitSendResult.Failed(presence.redactedMessage)
                is SimulatorPresenceResult.Present -> {
                    val outbound = try {
                        protocol.notice(circuit, packet)
                    } catch (ex: IllegalArgumentException) {
                        return SimulatorCircuitSendResult.Failed(NOTICE_PAYLOAD_INVALID)
                    }
                    val observations = GatewayNoticeObservationCollector()
                    val send = sendReliable(outbound, NOTICE_ACK_TIMEOUT, observations)
                    if (send != null) {
                        send
                    } else {
                        drainNoticeObservations(observations)
                        SimulatorCircuitSendResult.Sent(observations.redactedSummary())
                    }
                }
            }

        private fun requestArchive(circuit: SimulatorCircuit, groupId: String): SimulatorNoticeArchiveResult =
            when (val presence = ensurePresence(circuit)) {
                is SimulatorPresenceResult.Failed -> SimulatorNoticeArchiveResult.Failed(
                    status = presence.status.toArchiveStatus(),
                    redactedMessage = REDACTED_SEND_FAILURE,
                )
                is SimulatorPresenceResult.Present -> {
                    archiveReplies.remove(groupId)?.let { return SimulatorNoticeArchiveResult.Found(it) }
                    archiveMalformed = false
                    val outbound = try {
                        protocol.noticeArchiveRequest(circuit, groupId)
                    } catch (ex: IllegalArgumentException) {
                        return SimulatorNoticeArchiveResult.Failed(
                            status = SimulatorNoticeArchiveStatus.REQUEST_INVALID,
                            redactedMessage = REDACTED_SEND_FAILURE,
                        )
                    }
                    val sendFailure = sendReliable(outbound, REDACTED_SEND_FAILURE, null)
                    if (sendFailure != null) {
                        return SimulatorNoticeArchiveResult.Failed(
                            status = SimulatorNoticeArchiveStatus.REQUEST_SEND_FAILED,
                            redactedMessage = sendFailure.redactedMessage,
                        )
                    }
                    waitForArchive(groupId)
                }
            }

        private fun logout(circuit: SimulatorCircuit): SimulatorLogoutResult {
            val presence = ensurePresence(circuit)
            if (presence is SimulatorPresenceResult.Failed) {
                return Failed(presence.redactedMessage)
            }
            return try {
                send(protocol.logoutRequest(circuit))
                val loggedOut = waitForLogout()
                if (loggedOut) {
                    closeExchange()
                    LoggedOut
                } else {
                    runCatching { send(protocol.closeCircuit()) }
                    closeExchange()
                    ClosedWithoutReply
                }
            } catch (ex: Exception) {
                Failed(REDACTED_SEND_FAILURE)
            }
        }

        private fun waitForArchive(groupId: String): SimulatorNoticeArchiveResult {
            var sawWrongGroup = archiveReplies.keys.any { it != groupId }
            repeat(ARCHIVE_RECEIVE_ATTEMPTS) {
                archiveReplies.remove(groupId)?.let { return SimulatorNoticeArchiveResult.Found(it) }
                serviceInbound(ARCHIVE_RECEIVE_TIMEOUT_MILLIS, null)
                if (archiveReplies.keys.any { it != groupId }) {
                    sawWrongGroup = true
                }
            }
            val status = when {
                archiveMalformed -> SimulatorNoticeArchiveStatus.REPLY_MALFORMED
                sawWrongGroup -> SimulatorNoticeArchiveStatus.WRONG_GROUP_REPLY
                else -> SimulatorNoticeArchiveStatus.REPLY_TIMEOUT
            }
            return SimulatorNoticeArchiveResult.Failed(status, REDACTED_SEND_FAILURE)
        }

        private fun waitForLogout(): Boolean {
            repeat(LOGOUT_RECEIVE_ATTEMPTS) {
                if (serviceInbound(LOGOUT_RECEIVE_TIMEOUT_MILLIS, null).logoutMatched) {
                    return true
                }
            }
            return false
        }

        private fun sendReliable(
            outbound: SimulatorOutboundPacket,
            timeoutMessage: String,
            observations: GatewayNoticeObservationCollector?,
        ): SimulatorCircuitSendResult.Failed? {
            val sequenceNumber = outbound.reliableSequenceNumber ?: return try {
                send(outbound)
                null
            } catch (ex: Exception) {
                SimulatorCircuitSendResult.Failed(NOTICE_PACKET_SEND_FAILED)
            }
            var pending = PendingReliableSend(sequenceNumber, outbound, attempt = 1)
            while (true) {
                try {
                    send(pending.packet)
                } catch (ex: Exception) {
                    return SimulatorCircuitSendResult.Failed(NOTICE_PACKET_SEND_FAILED)
                }
                if (waitForAck(sequenceNumber, observations)) {
                    return null
                }
                when (val decision = protocol.onReliableSendTimeout(sequenceNumber)) {
                    is Resend -> {
                        captureHealth()
                        pending = PendingReliableSend(sequenceNumber, decision.packet, pending.attempt + 1)
                    }
                    is TimedOut -> {
                        captureHealth()
                        return SimulatorCircuitSendResult.Failed(timeoutMessage)
                    }
                    AwaitAck -> return null
                }
            }
        }

        private fun waitForAck(
            sequenceNumber: Long,
            observations: GatewayNoticeObservationCollector?,
        ): Boolean {
            var observedPackets = 0
            var receiveTimeouts = 0
            while (
                observedPackets < NOTICE_ACK_RECEIVE_PACKET_LIMIT &&
                receiveTimeouts < NOTICE_ACK_RECEIVE_TIMEOUT_LIMIT
            ) {
                val result = serviceInbound(NOTICE_ACK_RECEIVE_TIMEOUT_MILLIS, observations)
                if (result.payload == null) {
                    receiveTimeouts += 1
                    sendHeartbeatIfDue()
                    continue
                }
                observedPackets += 1
                if (sequenceNumber in result.outgoingAcks) {
                    return true
                }
                sendHeartbeatIfDue()
            }
            return false
        }

        private fun drainNoticeObservations(observations: GatewayNoticeObservationCollector) {
            repeat(NOTICE_POST_ACK_RECEIVE_ATTEMPTS) {
                serviceInbound(NOTICE_POST_ACK_RECEIVE_TIMEOUT_MILLIS, observations)
            }
        }

        private fun receiveUntil(
            wantedType: SimulatorPacketType,
            receiveTimeoutMillis: Int,
            maxAttempts: Int,
        ): ReceiveSearchResult {
            var observedPackets = 0
            repeat(maxAttempts) {
                val payload = serviceInbound(receiveTimeoutMillis, null).payload ?: return@repeat
                observedPackets += 1
                if (LibomvPacketCodec.packetType(payload) == wantedType) {
                    return ReceiveSearchResult.Found(payload)
                }
            }
            return ReceiveSearchResult.Failed(observedPackets)
        }

        private fun serviceInbound(
            timeoutMillis: Int,
            observations: GatewayNoticeObservationCollector?,
        ): ReceiveServiceResult {
            val currentExchange = exchange ?: return ReceiveServiceResult()
            val endpoint = activeEndpoint ?: return ReceiveServiceResult()
            val circuit = activeCircuit ?: return ReceiveServiceResult()
            val inbound = try {
                currentExchange.receive(endpoint, timeoutMillis)
            } catch (ex: Exception) {
                return ReceiveServiceResult()
            } ?: return ReceiveServiceResult()
            val actions = protocol.onInbound(circuit, inbound.payload)
            captureHealth()
            cacheArchiveReply(inbound.payload)
            val outgoingAcks = mutableListOf<Long>()
            var logoutMatched = false
            actions.forEach { action ->
                when (action) {
                    is AckReliable -> send(action.packet)
                    is ReplyToPing -> {
                        pingReplies += 1
                        send(action.packet)
                    }
                    is RecordOutgoingAck -> outgoingAcks += action.sequenceNumber
                    is MatchedArchiveReply -> archiveReplies[action.groupId] = action.entries
                    MatchedLogoutReply -> logoutMatched = true
                    is ObserveNoticeTraffic -> observations?.record(action.redactedSummary)
                    is MarkFailed,
                    IgnoreInbound,
                    -> Unit
                }
            }
            return ReceiveServiceResult(
                payload = inbound.payload,
                outgoingAcks = outgoingAcks,
                logoutMatched = logoutMatched,
            )
        }

        private fun cacheArchiveReply(payload: ByteArray) {
            if (LibomvPacketCodec.packetType(payload) != SimulatorPacketType.GROUP_NOTICES_LIST_REPLY) {
                return
            }
            val reply = LibomvPacketCodec.groupNoticesListReply(payload)
            if (reply == null) {
                archiveMalformed = true
            } else {
                archiveReplies[reply.groupId] = reply.entries
            }
        }

        private fun sendHeartbeatIfDue() {
            val circuit = activeCircuit ?: return
            if (protocol.health().status != SimulatorSessionHealthStatus.PRESENT) {
                return
            }
            val now = System.currentTimeMillis()
            if (now < nextHeartbeatAt) {
                return
            }
            runCatching { send(protocol.heartbeat(circuit)) }
            nextHeartbeatAt = now + HEARTBEAT_INTERVAL_MILLIS
        }

        private fun send(packet: SimulatorOutboundPacket) {
            captureHealth()
            send(packet.payload)
        }

        private fun send(payload: ByteArray) {
            val currentExchange = exchange ?: throw SimulatorPacketExchangeException()
            val endpoint = activeEndpoint ?: throw SimulatorPacketExchangeException()
            currentExchange.send(endpoint, listOf(payload))
        }

        private fun resetForCircuit(circuit: SimulatorCircuit) {
            if (activeCircuit == circuit && exchange != null) {
                return
            }
            closeExchange()
            exchange = packetExchangeFactory.create()
            activeCircuit = circuit
            activeEndpoint = SimulatorEndpoint(circuit.simulatorIp, circuit.simulatorPort)
            protocol = SimulatorSessionProtocol()
            captureHealth()
            cachedPresence = null
            pingReplies = 0
            archiveMalformed = false
            archiveReplies.clear()
        }

        private fun closeExchange() {
            (exchange as? AutoCloseable)?.runCatching { close() }
            exchange = null
            activeCircuit = null
            activeEndpoint = null
            cachedPresence = null
            protocol = SimulatorSessionProtocol()
            captureHealth()
            nextHeartbeatAt = 0L
        }

        private fun captureHealth() {
            latestHealth = protocol.health()
        }

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
    }

    private data class CachedPresence(
        val circuit: SimulatorCircuit,
        val regionName: String?,
        val regionProtocolFlags: RegionProtocolFlags,
    )

    private data class ReceiveServiceResult(
        val payload: ByteArray? = null,
        val outgoingAcks: List<Long> = emptyList(),
        val logoutMatched: Boolean = false,
    )

    private sealed interface ReceiveSearchResult {
        data class Found(val payload: ByteArray) : ReceiveSearchResult
        data class Failed(val observedPackets: Int) : ReceiveSearchResult
    }

    private class GatewayNoticeObservationCollector {
        private val observations = mutableListOf<String>()

        fun record(redactedSummary: String) {
            observations += redactedSummary
        }

        fun redactedSummary(): String = buildList {
            add("transportAck=passed")
            add("observedPackets=${observations.size}")
            if (observations.isNotEmpty()) {
                add("observations=${observations.take(MAX_REPORTED_OBSERVATIONS).joinToString("|")}")
            }
        }.joinToString("; ")

        private companion object {
            const val MAX_REPORTED_OBSERVATIONS = 6
        }
    }

    private companion object {
        const val REDACTED_SEND_FAILURE = "protocol simulator send failed"
        const val NOTICE_PAYLOAD_INVALID = "notice send packet invalid"
        const val NOTICE_PACKET_SEND_FAILED = "notice send packet transport failed"
        const val NOTICE_ACK_TIMEOUT = "notice send ack timeout after 3 attempts"
        const val WORKER_THREAD_NAME = "gem-simulator-session"
        const val COMMAND_TIMEOUT_MILLIS = 30_000L
        const val RECEIVE_POLL_MILLIS = 25
        const val HEARTBEAT_INTERVAL_MILLIS = 250L
        const val HANDSHAKE_RECEIVE_TIMEOUT_MILLIS = 2_000
        const val HANDSHAKE_RECEIVE_ATTEMPTS = 12
        const val MOVEMENT_RECEIVE_TIMEOUT_MILLIS = 250
        const val MOVEMENT_RECEIVE_ATTEMPTS = 12
        const val NOTICE_ACK_RECEIVE_TIMEOUT_MILLIS = 250
        const val NOTICE_ACK_RECEIVE_PACKET_LIMIT = 128
        const val NOTICE_ACK_RECEIVE_TIMEOUT_LIMIT = 24
        const val NOTICE_POST_ACK_RECEIVE_TIMEOUT_MILLIS = 250
        const val NOTICE_POST_ACK_RECEIVE_ATTEMPTS = 4
        const val ARCHIVE_RECEIVE_TIMEOUT_MILLIS = 250
        const val ARCHIVE_RECEIVE_ATTEMPTS = 24
        const val LOGOUT_RECEIVE_TIMEOUT_MILLIS = 250
        const val LOGOUT_RECEIVE_ATTEMPTS = 12
    }
}
