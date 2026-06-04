package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupListResult
import org.hostess.protocol.libomv.LibomvClientSession
import org.hostess.protocol.libomv.LibomvGroupSnapshot
import org.hostess.protocol.libomv.mapping.LibomvGroupMapping
import org.hostess.protocol.libomv.mapping.LibomvGroupMappingResult

class ProtocolGroupRuntime internal constructor(
    private val clientSession: LibomvClientSession,
    private val currentGroupsSource: CurrentGroupsSource = CurrentGroupsSource.unavailable(),
) {
    fun currentGroups(session: HostessSession): GroupListResult {
        val bindingFailure = clientSession.requireSession(session)
        if (bindingFailure != null) {
            return GroupListResult.Failure(bindingFailure.copy(reason = CoreFailureReason.GROUP_LIST_FAILED))
        }

        val fetched = currentGroupsSource.currentGroups(session)
        return when (fetched) {
            is CurrentGroupsFetchResult.Failure -> groupFailure("current groups unavailable")
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
    fun currentGroups(session: HostessSession): CurrentGroupsFetchResult

    companion object {
        fun unavailable(): CurrentGroupsSource =
            CurrentGroupsSource { CurrentGroupsFetchResult.Failure }
    }
}

internal sealed interface CurrentGroupsFetchResult {
    data class Success(val groups: List<LibomvGroupSnapshot>) : CurrentGroupsFetchResult
    data object Failure : CurrentGroupsFetchResult
}
