package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.AvatarPort
import org.hostess.core.ports.AvatarReadinessProof
import org.hostess.core.ports.AvatarReadinessProofStatus
import org.hostess.core.ports.AvatarReadinessResult
import org.hostess.protocol.libomv.runtime.ProtocolAvatarRuntime

class LibomvAvatarAdapter(
    private val avatarRuntime: ProtocolAvatarRuntime? = null,
) : AvatarPort {
    override fun ensureReady(session: HostessSession): AvatarReadinessResult =
        avatarRuntime?.ensureReady(session)
            ?: AvatarReadinessResult.Failure(
                proof = AvatarReadinessProof.notRun(AvatarReadinessProofStatus.RUNTIME_GAP),
                failure = CoreFailure(
                    reason = CoreFailureReason.AVATAR_READINESS_FAILED,
                    redactedMessage = "avatar readiness runtime unavailable",
                ),
            )
}
