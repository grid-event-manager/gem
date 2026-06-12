package org.gem.core.ports

import org.gem.core.domain.GemDelay
import org.gem.core.domain.GemInstant

interface ClockPort {
    fun now(): GemInstant

    fun pause(duration: GemDelay)
}
