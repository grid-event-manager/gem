package org.gem.core.services

import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GemSession
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.GroupPort
import org.gem.core.ports.SimulatorPresenceProofResult

class GroupDirectoryService(
    private val groupPort: GroupPort,
) {
    fun currentGroups(session: GemSession): GroupListResult = groupPort.currentGroups(session)

    fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult =
        groupPort.simulatorPresence(session)

    fun noticeArchive(session: GemSession, group: GroupMembership): GroupNoticeArchiveResult =
        groupPort.noticeArchive(session, group)
}
