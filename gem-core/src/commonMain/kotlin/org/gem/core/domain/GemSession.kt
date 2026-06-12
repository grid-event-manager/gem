package org.gem.core.domain

data class GemSession(
    val sessionId: SessionId,
    val accountLabel: AccountLabel,
    val startedAt: GemInstant,
    val isActive: Boolean,
)
