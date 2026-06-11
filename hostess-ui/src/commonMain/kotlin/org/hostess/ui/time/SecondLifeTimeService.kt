package org.hostess.ui.time

import org.hostess.core.ports.ClockPort

class SecondLifeTimeService(
    private val clockPort: ClockPort,
) {
    fun currentSnapshot(): SecondLifeTimeSnapshot {
        val now = clockPort.now()
        return SecondLifeTimeSnapshot(
            display = SecondLifeTimeFormatter.display(now),
            epochMilliseconds = now.epochMilliseconds,
        )
    }
}

data class SecondLifeTimeSnapshot(
    val display: SecondLifeTimeDisplay,
    val epochMilliseconds: Long,
)
