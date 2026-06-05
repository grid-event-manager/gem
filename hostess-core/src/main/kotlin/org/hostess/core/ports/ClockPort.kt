package org.hostess.core.ports

import org.hostess.core.domain.HostessDelay
import org.hostess.core.domain.HostessInstant

interface ClockPort {
    fun now(): HostessInstant

    fun pause(duration: HostessDelay)
}
