package org.gem.protocol.libomv

import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GemSession
import org.gem.core.ports.AvatarPort
import org.gem.core.ports.AvatarReadinessProof
import org.gem.core.ports.AvatarReadinessProofStatus
import org.gem.core.ports.AvatarReadinessResult
import org.gem.protocol.libomv.runtime.ProtocolAvatarRuntime

class LibomvAvatarAdapter(
    private val avatarRuntime: ProtocolAvatarRuntime? = null,
) : AvatarPort {
    override fun ensureReady(session: GemSession): AvatarReadinessResult =
        avatarRuntime?.ensureReady(session)
            ?: AvatarReadinessResult.Failure(
                proof = AvatarReadinessProof.notRun(AvatarReadinessProofStatus.RUNTIME_GAP),
                failure = CoreFailure(
                    reason = CoreFailureReason.AVATAR_READINESS_FAILED,
                    redactedMessage = "avatar readiness runtime unavailable",
                ),
            )
}
