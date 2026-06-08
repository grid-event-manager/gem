package org.hostess.core.services

import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.AvatarPort
import org.hostess.core.ports.AvatarReadinessResult

class AvatarReadinessService(
    private val avatarPort: AvatarPort,
) {
    fun ensureReady(session: HostessSession): AvatarReadinessResult =
        avatarPort.ensureReady(session)
}
