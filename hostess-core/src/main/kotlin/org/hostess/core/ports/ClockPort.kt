package org.hostess.core.ports

import java.time.Duration
import java.time.Instant

interface ClockPort {
    fun now(): Instant

    fun pause(duration: Duration)
}
