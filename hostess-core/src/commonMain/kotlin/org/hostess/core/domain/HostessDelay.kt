package org.hostess.core.domain

@JvmInline
value class HostessDelay(
    val milliseconds: Long,
) : Comparable<HostessDelay> {
    init {
        require(milliseconds >= 0L) { "Hostess delay cannot be negative." }
    }

    val isZero: Boolean
        get() = milliseconds == 0L

    val isPositive: Boolean
        get() = milliseconds > 0L

    override fun compareTo(other: HostessDelay): Int =
        milliseconds.compareTo(other.milliseconds)

    companion object {
        val ZERO: HostessDelay = HostessDelay(0L)

        fun ofMilliseconds(milliseconds: Long): HostessDelay =
            HostessDelay(milliseconds)

        fun ofSeconds(seconds: Long): HostessDelay {
            require(seconds >= 0L) { "Hostess delay cannot be negative." }
            require(seconds <= Long.MAX_VALUE / 1_000L) { "Hostess delay is too large." }
            return HostessDelay(seconds * 1_000L)
        }
    }
}
