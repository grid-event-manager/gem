package org.hostess.core.domain

import java.time.Instant

data class HostessSession(
    val sessionId: SessionId,
    val accountLabel: AccountLabel,
    val startedAt: Instant,
    val isActive: Boolean,
)
