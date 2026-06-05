package org.hostess.core.ports

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.HostessSession

interface GroupPort {
    fun currentGroups(session: HostessSession): GroupListResult
}

sealed interface GroupListResult {
    class Success(groups: List<GroupMembership>) : GroupListResult {
        val groups: List<GroupMembership> = groups.toList()
    }

    data class Failure(val failure: CoreFailure) : GroupListResult
}
