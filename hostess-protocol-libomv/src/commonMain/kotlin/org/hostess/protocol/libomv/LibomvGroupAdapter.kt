package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupPort
import org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime

class LibomvGroupAdapter(
    internal val clientSession: LibomvClientSession,
    private val groupRuntime: ProtocolGroupRuntime? = null,
) : GroupPort {
    override fun currentGroups(session: HostessSession): GroupListResult =
        groupRuntime?.currentGroups(session)
            ?: GroupListResult.Failure(clientSession.unavailable(CoreFailureReason.GROUP_LIST_FAILED))
}
