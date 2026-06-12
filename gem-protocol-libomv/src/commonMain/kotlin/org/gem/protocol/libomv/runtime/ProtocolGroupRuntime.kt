package org.gem.protocol.libomv.runtime

import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GemSession
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.SimulatorPresenceProof
import org.gem.core.ports.SimulatorPresenceProofResult
import org.gem.core.ports.SimulatorPresenceProofStatus
import org.gem.protocol.libomv.LibomvClientSession
import org.gem.protocol.libomv.LibomvGroupSnapshot
import org.gem.protocol.libomv.LibomvSessionIdentity
import org.gem.protocol.libomv.LibomvSessionIdentityResult
import org.gem.protocol.libomv.mapping.LibomvGroupMapping
import org.gem.protocol.libomv.mapping.LibomvGroupMappingResult

class ProtocolGroupRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val currentGroupsSource: CurrentGroupsSource = CurrentGroupsSource.unavailable(),
    private val simulatorPresenceSource: SimulatorPresenceSource = SimulatorPresenceSource.unavailable(),
    private val noticeArchiveSource: GroupNoticeArchiveSource = GroupNoticeArchiveSource.unavailable(),
) {
    fun currentGroups(session: GemSession): GroupListResult {
        val identity = when (val result = clientSession.requireIdentity(session)) {
            is LibomvSessionIdentityResult.Failure ->
                return GroupListResult.Failure(result.failure.copy(reason = CoreFailureReason.GROUP_LIST_FAILED))
            is LibomvSessionIdentityResult.Success -> result.identity
        }

        val fetched = currentGroupsSource.currentGroups(identity)
        return when (fetched) {
            is CurrentGroupsFetchResult.Failure -> groupFailure(fetched.redactedMessage)
            is CurrentGroupsFetchResult.Success -> mapCurrentGroups(fetched)
        }
    }

    fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult {
        val identity = when (val result = clientSession.requireIdentity(session)) {
            is LibomvSessionIdentityResult.Failure -> {
                val message = result.failure.redactedMessage ?: "simulator presence blocked"
                return SimulatorPresenceProofResult.Failure(
                    proof = blockedPresenceProof(message),
                    failure = result.failure.copy(reason = CoreFailureReason.GROUP_LIST_FAILED),
                )
            }
            is LibomvSessionIdentityResult.Success -> result.identity
        }
        return simulatorPresenceSource.simulatorPresence(identity)
    }

    fun noticeArchive(session: GemSession, group: GroupMembership): GroupNoticeArchiveResult {
        val identity = when (val result = clientSession.requireIdentity(session)) {
            is LibomvSessionIdentityResult.Failure -> {
                val message = result.failure.redactedMessage ?: "notice archive blocked"
                return GroupNoticeArchiveResult.Failure(
                    group = group,
                    failure = result.failure.copy(
                        reason = CoreFailureReason.GROUP_LIST_FAILED,
                        redactedMessage = message,
                    ),
                )
            }
            is LibomvSessionIdentityResult.Success -> result.identity
        }
        return noticeArchiveSource.noticeArchive(identity, group)
    }

    private fun mapCurrentGroups(result: CurrentGroupsFetchResult.Success): GroupListResult =
        when (val mapped = LibomvGroupMapping.currentGroups(result.groups)) {
            LibomvGroupMappingResult.Failure -> groupFailure("current groups invalid")
            is LibomvGroupMappingResult.Success -> GroupListResult.Success(mapped.groups)
        }

    private fun groupFailure(message: String): GroupListResult.Failure =
        GroupListResult.Failure(CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, redactedMessage = message))

    private fun blockedPresenceProof(message: String): SimulatorPresenceProof = SimulatorPresenceProof(
        simulatorPresenceStatus = SimulatorPresenceProofStatus.BLOCKED,
        regionHandshakeStatus = SimulatorPresenceProofStatus.NOT_RUN,
        regionHandshakeReplyStatus = SimulatorPresenceProofStatus.NOT_RUN,
        agentMovementStatus = SimulatorPresenceProofStatus.NOT_RUN,
        agentUpdateStatus = SimulatorPresenceProofStatus.NOT_RUN,
        redactedMessage = message,
    )
}

internal fun interface SimulatorPresenceSource {
    fun simulatorPresence(identity: LibomvSessionIdentity): SimulatorPresenceProofResult

    companion object {
        fun unavailable(): SimulatorPresenceSource = SimulatorPresenceSource {
            val message = "simulator presence runtime unavailable"
            SimulatorPresenceProofResult.Failure(
                proof = SimulatorPresenceProof(
                    simulatorPresenceStatus = SimulatorPresenceProofStatus.RUNTIME_GAP,
                    regionHandshakeStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    regionHandshakeReplyStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    agentMovementStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    agentUpdateStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    redactedMessage = message,
                ),
                failure = CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, redactedMessage = message),
            )
        }
    }
}

internal fun interface GroupNoticeArchiveSource {
    fun noticeArchive(identity: LibomvSessionIdentity, group: GroupMembership): GroupNoticeArchiveResult

    companion object {
        fun unavailable(): GroupNoticeArchiveSource = GroupNoticeArchiveSource { _, group ->
            val message = "notice archive runtime unavailable"
            GroupNoticeArchiveResult.Failure(
                group = group,
                failure = CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, redactedMessage = message),
            )
        }
    }
}

internal fun interface CurrentGroupsSource {
    fun currentGroups(identity: LibomvSessionIdentity): CurrentGroupsFetchResult

    companion object {
        fun unavailable(): CurrentGroupsSource = CurrentGroupsSource {
            CurrentGroupsFetchResult.Failure(
                status = CurrentGroupsFailureStatus.RUNTIME_GAP,
                redactedMessage = "current groups unavailable",
            )
        }
    }
}

internal sealed interface CurrentGroupsFetchResult {
    data class Success(val groups: List<LibomvGroupSnapshot>) : CurrentGroupsFetchResult
    data class Failure(
        val status: CurrentGroupsFailureStatus,
        val redactedMessage: String,
    ) : CurrentGroupsFetchResult
}

internal enum class CurrentGroupsFailureStatus {
    RUNTIME_GAP,
    TRANSPORT_GAP,
    PACKET_GAP,
    PROOF_GAP,
}
