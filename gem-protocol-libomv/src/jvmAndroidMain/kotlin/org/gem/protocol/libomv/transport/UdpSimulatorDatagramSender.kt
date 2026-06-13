package org.gem.protocol.libomv.transport

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.math.max
import kotlin.math.min

internal class UdpSimulatorDatagramSender(
    private val socket: DatagramSocket = DatagramSocket(),
) : SimulatorPacketExchange, AutoCloseable {
    init {
        recordSocketState("udp_socket_open")
    }

    override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
        try {
            val address = InetAddress.getByName(endpoint.host)
            SimulatorUdpDiagnosticTrail.record(
                "udp_send_start",
                "endpointHost" to endpoint.host,
                "endpointPort" to endpoint.port,
                "resolvedAddress" to address.hostAddress,
                "payloadCount" to payloads.size,
                "payloadBytes" to payloads.sumOf { it.size },
                *socketFields(),
            )
            payloads.forEach { payload ->
                SimulatorUdpDiagnosticTrail.record(
                    "udp_send_payload",
                    "packetType" to LibomvPacketCodec.packetType(payload).name,
                    "ackSequences" to LibomvPacketCodec.packetAckSequences(payload).orEmpty().joinToString(","),
                    "payloadBytes" to payload.size,
                    *socketFields(),
                )
                socket.send(DatagramPacket(payload, payload.size, address, endpoint.port))
            }
            SimulatorUdpDiagnosticTrail.record(
                "udp_send_complete",
                "endpointHost" to endpoint.host,
                "endpointPort" to endpoint.port,
                "resolvedAddress" to address.hostAddress,
                "payloadCount" to payloads.size,
                *socketFields(),
            )
        } catch (ex: Exception) {
            SimulatorUdpDiagnosticTrail.record(
                "udp_send_exception",
                "exception" to ex.javaClass.simpleName,
                *socketFields(),
            )
            throw SimulatorPacketExchangeException()
        }
    }

    override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? {
        val address = try {
            InetAddress.getByName(endpoint.host)
        } catch (ex: Exception) {
            SimulatorUdpDiagnosticTrail.record(
                "udp_receive_resolve_exception",
                "endpointHost" to endpoint.host,
                "endpointPort" to endpoint.port,
                "exception" to ex.javaClass.simpleName,
                *socketFields(),
            )
            throw SimulatorPacketExchangeException()
        }
        val startedAt = System.nanoTime()
        val timeoutNanos = max(timeoutMillis, MIN_TIMEOUT_MILLIS).toLong() * NANOS_PER_MILLISECOND
        val deadline = startedAt + timeoutNanos
        val buffer = ByteArray(MAX_DATAGRAM_BYTES)
        SimulatorUdpDiagnosticTrail.record(
            "udp_receive_start",
            "endpointHost" to endpoint.host,
            "endpointPort" to endpoint.port,
            "resolvedAddress" to address.hostAddress,
            "timeoutMillis" to timeoutMillis,
            *socketFields(),
        )

        while (!socket.isClosed) {
            val remainingMillis = remainingMillis(deadline)
            if (remainingMillis <= 0) {
                SimulatorUdpDiagnosticTrail.record(
                    "udp_receive_deadline",
                    "endpointHost" to endpoint.host,
                    "endpointPort" to endpoint.port,
                    *socketFields(),
                )
                return null
            }
            socket.soTimeout = remainingMillis
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (ex: SocketTimeoutException) {
                SimulatorUdpDiagnosticTrail.record(
                    "udp_receive_timeout",
                    "endpointHost" to endpoint.host,
                    "endpointPort" to endpoint.port,
                    "remainingMillis" to remainingMillis,
                    *socketFields(),
                )
                return null
            } catch (ex: Exception) {
                SimulatorUdpDiagnosticTrail.record(
                    "udp_receive_exception",
                    "endpointHost" to endpoint.host,
                    "endpointPort" to endpoint.port,
                    "exception" to ex.javaClass.simpleName,
                    *socketFields(),
                )
                throw SimulatorPacketExchangeException()
            }
            val sourceAddressMatches = packet.address == address
            val sourcePortMatches = packet.port == endpoint.port
            val payload = packet.data.copyOf(packet.length)
            val accepted = sourceAddressMatches && sourcePortMatches
            SimulatorUdpDiagnosticTrail.record(
                "udp_receive_packet",
                "endpointHost" to endpoint.host,
                "endpointPort" to endpoint.port,
                "resolvedAddress" to address.hostAddress,
                "sourceAddress" to packet.address.hostAddress,
                "sourcePort" to packet.port,
                "datagramBytes" to packet.length,
                "packetType" to LibomvPacketCodec.packetType(payload).name,
                "ackSequences" to LibomvPacketCodec.packetAckSequences(payload).orEmpty().joinToString(","),
                "accepted" to accepted,
                "reason" to receiveReason(sourceAddressMatches, sourcePortMatches),
                *socketFields(),
            )
            if (accepted) {
                return SimulatorInboundPacket(
                    endpoint = endpoint,
                    payload = payload,
                )
            }
        }
        SimulatorUdpDiagnosticTrail.record(
            "udp_receive_socket_closed",
            "endpointHost" to endpoint.host,
            "endpointPort" to endpoint.port,
        )
        return null
    }

    override fun close() {
        recordSocketState("udp_socket_close")
        socket.close()
    }

    private fun recordSocketState(event: String) {
        SimulatorUdpDiagnosticTrail.record(event, *socketFields())
    }

    private fun socketFields(): Array<Pair<String, Any?>> =
        arrayOf(
            "localAddress" to runCatching { socket.localAddress?.hostAddress }.getOrNull(),
            "localPort" to runCatching { socket.localPort }.getOrNull(),
            "receiveBufferSize" to runCatching { socket.receiveBufferSize }.getOrNull(),
            "sendBufferSize" to runCatching { socket.sendBufferSize }.getOrNull(),
            "reuseAddress" to runCatching { socket.reuseAddress }.getOrNull(),
            "isBound" to socket.isBound,
            "isConnected" to socket.isConnected,
            "isClosed" to socket.isClosed,
        )

    private fun receiveReason(sourceAddressMatches: Boolean, sourcePortMatches: Boolean): String =
        when {
            sourceAddressMatches && sourcePortMatches -> "accepted"
            !sourceAddressMatches && !sourcePortMatches -> "source_address_and_port_mismatch"
            !sourceAddressMatches -> "source_address_mismatch"
            else -> "source_port_mismatch"
        }

    private fun remainingMillis(deadline: Long): Int {
        val remainingNanos = deadline - System.nanoTime()
        if (remainingNanos <= 0) {
            return 0
        }
        val millis = (remainingNanos / NANOS_PER_MILLISECOND).toInt()
        return min(max(millis, MIN_TIMEOUT_MILLIS), Int.MAX_VALUE)
    }

    private companion object {
        const val MAX_DATAGRAM_BYTES = 4096
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val MIN_TIMEOUT_MILLIS = 1
    }
}
