package org.gem.core.domain

@JvmInline
value class GemDelay(
    val milliseconds: Long,
) : Comparable<GemDelay> {
    init {
        require(milliseconds >= 0L) { "Gem delay cannot be negative." }
    }

    val isZero: Boolean
        get() = milliseconds == 0L

    val isPositive: Boolean
        get() = milliseconds > 0L

    override fun compareTo(other: GemDelay): Int =
        milliseconds.compareTo(other.milliseconds)

    companion object {
        val ZERO: GemDelay = GemDelay(0L)

        fun ofMilliseconds(milliseconds: Long): GemDelay =
            GemDelay(milliseconds)

        fun ofSeconds(seconds: Long): GemDelay {
            require(seconds >= 0L) { "Gem delay cannot be negative." }
            require(seconds <= Long.MAX_VALUE / 1_000L) { "Gem delay is too large." }
            return GemDelay(seconds * 1_000L)
        }
    }
}
