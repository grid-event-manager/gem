package org.hostess.protocol.libomv.transport

internal fun interface SimulatorPacketExchangeFactory {
    fun create(): SimulatorPacketExchange
}
