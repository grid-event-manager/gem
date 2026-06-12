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
    override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
        try {
            val address = InetAddress.getByName(endpoint.host)
            payloads.forEach { payload ->
                socket.send(DatagramPacket(payload, payload.size, address, endpoint.port))
            }
        } catch (ex: Exception) {
            throw SimulatorPacketExchangeException()
        }
    }

    override fun receive(endpoint: SimulatorEndpoint, timeoutMillis: Int): SimulatorInboundPacket? {
        val address = try {
            InetAddress.getByName(endpoint.host)
        } catch (ex: Exception) {
            throw SimulatorPacketExchangeException()
        }
        val startedAt = System.nanoTime()
        val timeoutNanos = max(timeoutMillis, MIN_TIMEOUT_MILLIS).toLong() * NANOS_PER_MILLISECOND
        val deadline = startedAt + timeoutNanos
        val buffer = ByteArray(MAX_DATAGRAM_BYTES)

        while (!socket.isClosed) {
            val remainingMillis = remainingMillis(deadline)
            if (remainingMillis <= 0) {
                return null
            }
            socket.soTimeout = remainingMillis
            val packet = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(packet)
            } catch (ex: SocketTimeoutException) {
                return null
            } catch (ex: Exception) {
                throw SimulatorPacketExchangeException()
            }
            if (packet.address == address && packet.port == endpoint.port) {
                return SimulatorInboundPacket(
                    endpoint = endpoint,
                    payload = packet.data.copyOf(packet.length),
                )
            }
        }
        return null
    }

    override fun close() {
        socket.close()
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
