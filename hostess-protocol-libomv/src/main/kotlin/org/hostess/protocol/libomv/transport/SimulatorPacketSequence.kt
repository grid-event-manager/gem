package org.hostess.protocol.libomv.transport

internal class SimulatorPacketSequence(
    initialValue: Int = 0,
) {
    private var current = initialValue

    init {
        require(initialValue >= 0)
    }

    fun next(): Int {
        require(current < Int.MAX_VALUE)
        current += 1
        return current
    }
}
