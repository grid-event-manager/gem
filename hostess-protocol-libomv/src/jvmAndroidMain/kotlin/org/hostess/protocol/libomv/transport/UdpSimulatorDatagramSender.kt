package org.hostess.protocol.libomv.transport

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

internal class UdpSimulatorDatagramSender(
    private val socket: DatagramSocket = DatagramSocket(),
) : SimulatorPacketSender, AutoCloseable {
    override fun send(endpoint: SimulatorEndpoint, payloads: List<ByteArray>) {
        val address = InetAddress.getByName(endpoint.host)
        payloads.forEach { payload ->
            socket.send(DatagramPacket(payload, payload.size, address, endpoint.port))
        }
    }

    override fun close() {
        socket.close()
    }
}
