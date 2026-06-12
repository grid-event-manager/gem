package org.gem.protocol.libomv

import org.gem.core.domain.CoreFailure
import org.gem.core.domain.CoreFailureReason
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GemSession
import org.gem.core.ports.GroupListResult
import org.gem.core.ports.GroupNoticeArchiveResult
import org.gem.core.ports.GroupPort
import org.gem.core.ports.SimulatorPresenceProof
import org.gem.core.ports.SimulatorPresenceProofResult
import org.gem.core.ports.SimulatorPresenceProofStatus
import org.gem.protocol.libomv.runtime.ProtocolGroupRuntime

class LibomvGroupAdapter(
    internal val clientSession: LibomvClientSession,
    private val groupRuntime: ProtocolGroupRuntime? = null,
) : GroupPort {
    override fun currentGroups(session: GemSession): GroupListResult =
        groupRuntime?.currentGroups(session)
            ?: GroupListResult.Failure(clientSession.unavailable(CoreFailureReason.GROUP_LIST_FAILED))

    override fun simulatorPresence(session: GemSession): SimulatorPresenceProofResult =
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

    override fun noticeArchive(session: GemSession, group: GroupMembership): GroupNoticeArchiveResult =
        groupRuntime?.noticeArchive(session, group)
            ?: GroupNoticeArchiveResult.Failure(
                group = group,
                failure = CoreFailure(
                    reason = CoreFailureReason.GROUP_LIST_FAILED,
                    redactedMessage = "notice archive runtime unavailable",
                ),
            )
}
