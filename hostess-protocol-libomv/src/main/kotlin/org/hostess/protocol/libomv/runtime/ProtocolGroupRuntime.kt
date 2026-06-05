package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupListResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvGroupSnapshot
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.LibomvSessionIdentityResult
import org.hostess.protocol.libomv.mapping.LibomvGroupMapping
import org.hostess.protocol.libomv.mapping.LibomvGroupMappingResult

class ProtocolGroupRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val currentGroupsSource: CurrentGroupsSource = CurrentGroupsSource.unavailable(),
) {
    fun currentGroups(session: HostessSession): GroupListResult {
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

    private fun mapCurrentGroups(result: CurrentGroupsFetchResult.Success): GroupListResult =
        when (val mapped = LibomvGroupMapping.currentGroups(result.groups)) {
            LibomvGroupMappingResult.Failure -> groupFailure("current groups invalid")
            is LibomvGroupMappingResult.Success -> GroupListResult.Success(mapped.groups)
        }

    private fun groupFailure(message: String): GroupListResult.Failure =
        GroupListResult.Failure(CoreFailure(CoreFailureReason.GROUP_LIST_FAILED, redactedMessage = message))
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
