package org.gem.protocol.libomv.transport

internal fun interface SimulatorPacketExchangeFactory {
    fun create(): SimulatorPacketExchange
}
