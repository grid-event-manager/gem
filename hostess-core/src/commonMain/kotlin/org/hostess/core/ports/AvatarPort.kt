package org.hostess.core.ports

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.HostessSession

interface AvatarPort {
    fun ensureReady(session: HostessSession): AvatarReadinessResult
}

sealed interface AvatarReadinessResult {
    data class Success(val proof: AvatarReadinessProof) : AvatarReadinessResult
    data class Failure(
        val proof: AvatarReadinessProof,
        val failure: CoreFailure,
    ) : AvatarReadinessResult
}

data class AvatarReadinessProof(
    val avatarReadinessStatus: AvatarReadinessProofStatus,
    val simulatorPresenceStatus: AvatarReadinessProofStatus,
    val regionProtocolStatus: AvatarReadinessProofStatus,
    val agentAppearanceServiceStatus: AvatarReadinessProofStatus,
    val cofVersionStatus: AvatarReadinessProofStatus,
    val serverAppearanceStatus: AvatarReadinessProofStatus,
    val regionName: String? = null,
) {
    companion object {
        fun notRun(
            avatarReadinessStatus: AvatarReadinessProofStatus = AvatarReadinessProofStatus.NOT_RUN,
        ): AvatarReadinessProof = AvatarReadinessProof(
            avatarReadinessStatus = avatarReadinessStatus,
            simulatorPresenceStatus = AvatarReadinessProofStatus.NOT_RUN,
            regionProtocolStatus = AvatarReadinessProofStatus.NOT_RUN,
            agentAppearanceServiceStatus = AvatarReadinessProofStatus.NOT_RUN,
            cofVersionStatus = AvatarReadinessProofStatus.NOT_RUN,
            serverAppearanceStatus = AvatarReadinessProofStatus.NOT_RUN,
        )

        fun success(regionName: String? = null): AvatarReadinessProof = AvatarReadinessProof(
            avatarReadinessStatus = AvatarReadinessProofStatus.PASSED,
            simulatorPresenceStatus = AvatarReadinessProofStatus.PASSED,
            regionProtocolStatus = AvatarReadinessProofStatus.PASSED,
            agentAppearanceServiceStatus = AvatarReadinessProofStatus.PASSED,
            cofVersionStatus = AvatarReadinessProofStatus.PASSED,
            serverAppearanceStatus = AvatarReadinessProofStatus.PASSED,
            regionName = regionName,
        )
    }
}

enum class AvatarReadinessProofStatus(val reportValue: String) {
    PASSED("passed"),
    BLOCKED("blocked"),
    TRANSPORT_GAP("transport_gap"),
    RUNTIME_GAP("runtime_gap"),
    PROOF_GAP("proof_gap"),
    NOT_APPLICABLE("not_applicable"),
    NOT_RUN("not_run"),
}
