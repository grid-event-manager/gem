package org.hostess.protocol.libomv.transport

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class UdpSimulatorDatagramSenderTest {
    @Test
    fun `reuses source port across simulator sends`() {
        DatagramSocket(0, InetAddress.getByName(LOOPBACK)).use { receiver ->
            receiver.soTimeout = 2_000
            val sender = UdpSimulatorDatagramSender()
            try {
                sender.send(SimulatorEndpoint(LOOPBACK, receiver.localPort), listOf(byteArrayOf(1)))
                sender.send(SimulatorEndpoint(LOOPBACK, receiver.localPort), listOf(byteArrayOf(2)))

                val first = receiver.receivePayload()
                val second = receiver.receivePayload()

                assertContentEquals(byteArrayOf(1), first.body)
                assertContentEquals(byteArrayOf(2), second.body)
                assertEquals(first.sourcePort, second.sourcePort)
            } finally {
                sender.close()
            }
        }
    }

    private fun DatagramSocket.receivePayload(): ReceivedDatagram {
        val buffer = ByteArray(16)
        val packet = DatagramPacket(buffer, buffer.size)
        receive(packet)
        return ReceivedDatagram(
            sourcePort = packet.port,
            body = packet.data.copyOf(packet.length),
        )
    }

    private data class ReceivedDatagram(
        val sourcePort: Int,
        val body: ByteArray,
    )

    private companion object {
        const val LOOPBACK = "127.0.0.1"
    }
}
