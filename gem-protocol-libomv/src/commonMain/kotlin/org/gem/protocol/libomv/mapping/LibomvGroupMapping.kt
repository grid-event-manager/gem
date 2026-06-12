package org.gem.protocol.libomv.mapping

import org.gem.core.domain.GroupMembership
import org.gem.protocol.libomv.LibomvGroupSnapshot
import org.gem.protocol.libomv.LibomvMapping

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
