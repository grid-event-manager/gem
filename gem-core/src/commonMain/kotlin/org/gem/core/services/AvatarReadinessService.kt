package org.gem.core.services

import org.gem.core.domain.GemSession
import org.gem.core.ports.AvatarPort
import org.gem.core.ports.AvatarReadinessResult

class AvatarReadinessService(
    private val avatarPort: AvatarPort,
) {
    fun ensureReady(session: GemSession): AvatarReadinessResult =
        avatarPort.ensureReady(session)
}
