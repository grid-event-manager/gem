package org.hostess.core.services

import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupPort

class GroupDirectoryService(
    private val groupPort: GroupPort,
) {
    fun currentGroups(session: HostessSession): GroupListResult = groupPort.currentGroups(session)
}
