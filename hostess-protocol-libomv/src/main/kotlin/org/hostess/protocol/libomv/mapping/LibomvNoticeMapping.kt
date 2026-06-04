package org.hostess.protocol.libomv.mapping

import java.util.UUID
import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentRef
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.HostessSession
import org.hostess.core.domain.NoticeDraft
import org.hostess.protocol.libomv.LibomvSessionIdentity

internal data class LibomvNoticePacket(
    val agentId: String,
    val sessionId: String,
    val fromGroup: Boolean,
    val targetGroupId: String,
    val fromAgentName: String,
    val message: String,
    val dialog: Int,
    val offline: Int,
    val instantMessageId: String,
    val parentEstateId: Int,
    val timestamp: Int,
    val regionId: String,
    val position: LibomvNoticePosition,
    val attachment: LibomvNoticeAttachment?,
    val binaryBucket: ByteArray,
)

internal data class LibomvNoticeAttachment(
    val itemId: String,
    val ownerId: String,
    val kind: AttachmentKind,
)

internal data class LibomvNoticePosition(
    val x: Double,
    val y: Double,
    val z: Double,
) {
    companion object {
        val ZERO: LibomvNoticePosition = LibomvNoticePosition(0.0, 0.0, 0.0)
    }
}

internal sealed interface LibomvNoticeMappingResult {
    data class Success(val packet: LibomvNoticePacket) : LibomvNoticeMappingResult
    data object Failure : LibomvNoticeMappingResult
}

internal object LibomvNoticeMapping {
    const val GROUP_NOTICE_DIALOG: Int = 32
    const val ONLINE: Int = 0
    const val PARENT_ESTATE_ID: Int = 0
    const val TIMESTAMP: Int = 0
    const val ZERO_UUID: String = "00000000-0000-0000-0000-000000000000"

    fun noticePacket(
        identity: LibomvSessionIdentity,
        session: HostessSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): LibomvNoticeMappingResult {
        if (draft.subject.isBlank() || draft.attachments.size > 1) {
            return LibomvNoticeMappingResult.Failure
        }
        val agentId = uuidOrNull(identity.agentId) ?: return LibomvNoticeMappingResult.Failure
        val sessionId = uuidOrNull(identity.sessionId) ?: return LibomvNoticeMappingResult.Failure
        val targetGroupId = uuidOrNull(group.groupId.value) ?: return LibomvNoticeMappingResult.Failure
        val noticeAttachment = attachment?.let(::noticeAttachment)
            ?: if (attachment == null) null else return LibomvNoticeMappingResult.Failure
        val bucket = noticeAttachment?.let(::attachmentBucket) ?: ByteArray(0)

        return LibomvNoticeMappingResult.Success(
            LibomvNoticePacket(
                agentId = agentId.toString(),
                sessionId = sessionId.toString(),
                fromGroup = false,
                targetGroupId = targetGroupId.toString(),
                fromAgentName = session.accountLabel.value,
                message = "${draft.subject}|${draft.message}",
                dialog = GROUP_NOTICE_DIALOG,
                offline = ONLINE,
                instantMessageId = xorUuid(targetGroupId, agentId).toString(),
                parentEstateId = PARENT_ESTATE_ID,
                timestamp = TIMESTAMP,
                regionId = ZERO_UUID,
                position = LibomvNoticePosition.ZERO,
                attachment = noticeAttachment,
                binaryBucket = bucket,
            ),
        )
    }

    private fun noticeAttachment(attachment: AttachmentRef): LibomvNoticeAttachment? {
        val itemId = uuidOrNull(attachment.attachmentId.value) ?: return null
        val ownerId = uuidOrNull(attachment.ownerId.value) ?: return null
        return LibomvNoticeAttachment(
            itemId = itemId.toString(),
            ownerId = ownerId.toString(),
            kind = attachment.kind,
        )
    }

    private fun attachmentBucket(attachment: LibomvNoticeAttachment): ByteArray = buildString {
        append("<llsd><map>")
        append("<key>item_id</key><uuid>").append(attachment.itemId).append("</uuid>")
        append("<key>owner_id</key><uuid>").append(attachment.ownerId).append("</uuid>")
        append("</map></llsd>")
    }.encodeToByteArray()

    private fun uuidOrNull(value: String): UUID? = try {
        UUID.fromString(value)
    } catch (ex: IllegalArgumentException) {
        null
    }

    private fun xorUuid(left: UUID, right: UUID): UUID = UUID(
        left.mostSignificantBits xor right.mostSignificantBits,
        left.leastSignificantBits xor right.leastSignificantBits,
    )
}
