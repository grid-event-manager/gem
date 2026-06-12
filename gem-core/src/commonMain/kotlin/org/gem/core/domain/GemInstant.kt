package org.gem.core.domain

@JvmInline
value class GemInstant(
    val epochMilliseconds: Long,
) {
    companion object {
        val EPOCH: GemInstant = GemInstant(0L)
    }
}
