package org.hostess.protocol.libomv.runtime

import org.hostess.core.domain.CoreFailure
import org.hostess.core.domain.CoreFailureReason
import org.hostess.core.domain.GroupMembership
import org.hostess.core.ports.GroupNoticeArchiveEntry
import org.hostess.core.ports.GroupNoticeArchiveResult
import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.transport.ProtocolSimulatorCircuitClient
import org.hostess.protocol.libomv.transport.SimulatorNoticeArchiveResult
import org.hostess.protocol.libomv.transport.SimulatorNoticeArchiveStatus
import org.hostess.protocol.libomv.transport.toSimulatorCircuit

internal class ProtocolGroupNoticeArchiveSource(
    private val circuitClient: ProtocolSimulatorCircuitClient,
) : GroupNoticeArchiveSource {
    override fun noticeArchive(
        identity: LibomvSessionIdentity,
        group: GroupMembership,
    ): GroupNoticeArchiveResult =
        when (
            val result = circuitClient.requestGroupNoticeArchive(
                circuit = identity.toSimulatorCircuit(),
                groupId = group.groupId.value,
            )
        ) {
            is SimulatorNoticeArchiveResult.Found -> GroupNoticeArchiveResult.Success(
                group = group,
                entries = result.entries.map { entry ->
                    GroupNoticeArchiveEntry(
                        subject = entry.subject,
                        fromName = entry.fromName,
                        timestamp = entry.timestamp,
                        hasAttachment = entry.hasAttachment,
                        assetType = entry.assetType,
                    )
                },
            )
            is SimulatorNoticeArchiveResult.Failed -> GroupNoticeArchiveResult.Failure(
                group = group,
                failure = CoreFailure(
                    reason = CoreFailureReason.GROUP_LIST_FAILED,
                    redactedMessage = result.status.redactedMessage(),
                ),
            )
        }

    private fun SimulatorNoticeArchiveStatus.redactedMessage(): String =
        when (this) {
            SimulatorNoticeArchiveStatus.REQUEST_INVALID -> "notice archive blocked"
            SimulatorNoticeArchiveStatus.PRESENCE_TRANSPORT_GAP,
            SimulatorNoticeArchiveStatus.REQUEST_SEND_FAILED,
            -> "notice archive transport_gap"
            SimulatorNoticeArchiveStatus.PRESENCE_PROOF_GAP,
            SimulatorNoticeArchiveStatus.REPLY_TIMEOUT,
            SimulatorNoticeArchiveStatus.REPLY_MALFORMED,
            SimulatorNoticeArchiveStatus.WRONG_GROUP_REPLY,
            -> "notice archive proof_gap"
        }
}
