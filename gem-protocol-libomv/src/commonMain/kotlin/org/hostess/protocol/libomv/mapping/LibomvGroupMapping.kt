package org.hostess.protocol.libomv.mapping

import org.hostess.core.domain.GroupMembership
import org.hostess.protocol.libomv.LibomvGroupSnapshot
import org.hostess.protocol.libomv.LibomvMapping

internal sealed interface LibomvGroupMappingResult {
    data class Success(val groups: List<GroupMembership>) : LibomvGroupMappingResult
    data object Failure : LibomvGroupMappingResult
}

internal object LibomvGroupMapping {
    fun currentGroups(snapshots: List<LibomvGroupSnapshot>): LibomvGroupMappingResult {
        val seenGroupIds = linkedSetOf<String>()
        val groups = mutableListOf<GroupMembership>()
        for (snapshot in snapshots) {
            if (snapshot.groupId.isBlank() || snapshot.displayName.isBlank() || !seenGroupIds.add(snapshot.groupId)) {
                return LibomvGroupMappingResult.Failure
            }
            groups += LibomvMapping.groupMembership(snapshot)
        }
        return LibomvGroupMappingResult.Success(groups)
    }
}
