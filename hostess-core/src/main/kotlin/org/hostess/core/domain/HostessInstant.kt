package org.hostess.core.domain

@JvmInline
value class HostessInstant(
    val epochMilliseconds: Long,
) {
    companion object {
        val EPOCH: HostessInstant = HostessInstant(0L)
    }
}
