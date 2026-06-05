package org.hostess.core.domain

data class HostessSession(
    val sessionId: SessionId,
    val accountLabel: AccountLabel,
    val startedAt: HostessInstant,
    val isActive: Boolean,
)
