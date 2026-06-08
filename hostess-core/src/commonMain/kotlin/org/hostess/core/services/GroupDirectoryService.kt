package org.hostess.core.services

import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.SimulatorPresenceProofResult

class GroupDirectoryService(
    private val groupPort: GroupPort,
) {
    fun currentGroups(session: HostessSession): GroupListResult = groupPort.currentGroups(session)

    fun simulatorPresence(session: HostessSession): SimulatorPresenceProofResult =
        groupPort.simulatorPresence(session)

    fun noticeArchive(session: HostessSession, group: GroupMembership): GroupNoticeArchiveResult =
        groupPort.noticeArchive(session, group)
}
