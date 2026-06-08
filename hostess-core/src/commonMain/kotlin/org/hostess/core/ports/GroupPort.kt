package org.hostess.core.ports

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.HostessSession

interface GroupPort {
    fun currentGroups(session: HostessSession): GroupListResult
    fun simulatorPresence(session: HostessSession): SimulatorPresenceProofResult
    fun noticeArchive(session: HostessSession, group: GroupMembership): GroupNoticeArchiveResult
}

sealed interface GroupListResult {
    class Success(groups: List<GroupMembership>) : GroupListResult {
        val groups: List<GroupMembership> = groups.toList()
    }

    data class Failure(val failure: CoreFailure) : GroupListResult
}

sealed interface SimulatorPresenceProofResult {
    data class Success(val proof: SimulatorPresenceProof) : SimulatorPresenceProofResult
    data class Failure(
        val proof: SimulatorPresenceProof,
        val failure: CoreFailure,
    ) : SimulatorPresenceProofResult
}

data class SimulatorPresenceProof(
    val simulatorPresenceStatus: SimulatorPresenceProofStatus,
    val regionHandshakeStatus: SimulatorPresenceProofStatus,
    val regionHandshakeReplyStatus: SimulatorPresenceProofStatus,
    val agentMovementStatus: SimulatorPresenceProofStatus,
    val agentUpdateStatus: SimulatorPresenceProofStatus,
    val pingReplies: Int = 0,
    val redactedMessage: String? = null,
)

enum class SimulatorPresenceProofStatus(val reportValue: String) {
    PASSED("passed"),
    TRANSPORT_GAP("transport_gap"),
    PROOF_GAP("proof_gap"),
    RUNTIME_GAP("runtime_gap"),
    BLOCKED("blocked"),
    FAILED("failed"),
    NOT_RUN("not_run"),
}

sealed interface GroupNoticeArchiveResult {
    data class Success(
        val group: GroupMembership,
        val entries: List<GroupNoticeArchiveEntry>,
    ) : GroupNoticeArchiveResult

    data class Failure(
        val group: GroupMembership,
        val failure: CoreFailure,
    ) : GroupNoticeArchiveResult
}

data class GroupNoticeArchiveEntry(
    val subject: String,
    val fromName: String,
    val timestamp: Long?,
    val hasAttachment: Boolean,
    val assetType: Int?,
)
