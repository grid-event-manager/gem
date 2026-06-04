package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupPort

class LibomvGroupAdapter(
    internal val clientSession: LibomvClientSession,
) : GroupPort {
    override fun currentGroups(session: HostessSession): GroupListResult =
        GroupListResult.Failure(clientSession.unavailable(CoreFailureReason.GROUP_LIST_FAILED))
}
