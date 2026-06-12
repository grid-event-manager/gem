package org.hostess.protocol.libomv

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.HostessSession
import org.hostess.core.ports.GroupListResult
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.core.ports.GroupPort
import org.hostess.core.ports.SimulatorPresenceProof
import org.hostess.core.ports.SimulatorPresenceProofResult
import org.hostess.core.ports.SimulatorPresenceProofStatus
import org.hostess.protocol.libomv.runtime.ProtocolGroupRuntime

class LibomvGroupAdapter(
    internal val clientSession: LibomvClientSession,
    private val groupRuntime: ProtocolGroupRuntime? = null,
) : GroupPort {
    override fun currentGroups(session: HostessSession): GroupListResult =
        groupRuntime?.currentGroups(session)
            ?: GroupListResult.Failure(clientSession.unavailable(CoreFailureReason.GROUP_LIST_FAILED))

    override fun simulatorPresence(session: HostessSession): SimulatorPresenceProofResult =
        groupRuntime?.simulatorPresence(session)
            ?: SimulatorPresenceProofResult.Failure(
                proof = SimulatorPresenceProof(
                    simulatorPresenceStatus = SimulatorPresenceProofStatus.RUNTIME_GAP,
                    regionHandshakeStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    regionHandshakeReplyStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    agentMovementStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    agentUpdateStatus = SimulatorPresenceProofStatus.NOT_RUN,
                    redactedMessage = "simulator presence runtime unavailable",
                ),
                failure = clientSession.unavailable(CoreFailureReason.GROUP_LIST_FAILED),
            )

    override fun noticeArchive(session: HostessSession, group: GroupMembership): GroupNoticeArchiveResult =
        groupRuntime?.noticeArchive(session, group)
            ?: GroupNoticeArchiveResult.Failure(
                group = group,
                failure = CoreFailure(
                    reason = CoreFailureReason.GROUP_LIST_FAILED,
                    redactedMessage = "notice archive runtime unavailable",
                ),
            )
}
