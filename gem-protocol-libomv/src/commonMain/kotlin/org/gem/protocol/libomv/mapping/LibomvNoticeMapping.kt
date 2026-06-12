package org.gem.protocol.libomv.mapping

import org.gem.core.domain.AttachmentKind
import org.gem.core.domain.AttachmentRef
import org.gem.core.domain.GroupMembership
import org.gem.core.domain.GemSession
import org.gem.core.domain.NoticeDraft
import org.gem.protocol.libomv.LibomvSessionIdentity

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
    const val ZERO_ID: String = LibomvUuidCodec.ZERO_ID

    fun noticePacket(
        identity: LibomvSessionIdentity,
        session: GemSession,
        group: GroupMembership,
        draft: NoticeDraft,
        attachment: AttachmentRef?,
    ): LibomvNoticeMappingResult {
        if (draft.subject.isBlank() || draft.attachments.size > 1) {
            return LibomvNoticeMappingResult.Failure
        }
        val agentId = LibomvUuidCodec.canonicalOrNull(identity.agentId) ?: return LibomvNoticeMappingResult.Failure
        val sessionId = LibomvUuidCodec.canonicalOrNull(identity.sessionId) ?: return LibomvNoticeMappingResult.Failure
        val targetGroupId = LibomvUuidCodec.canonicalOrNull(group.groupId.value)
            ?: return LibomvNoticeMappingResult.Failure
        val noticeAttachment = attachment?.let { noticeAttachment(it, agentId) }
            ?: if (attachment == null) null else return LibomvNoticeMappingResult.Failure
        val bucket = noticeAttachment?.let(::attachmentBucket) ?: ByteArray(0)

        return LibomvNoticeMappingResult.Success(
            LibomvNoticePacket(
                agentId = agentId,
                sessionId = sessionId,
                fromGroup = false,
                targetGroupId = targetGroupId,
                fromAgentName = identity.agentName?.takeIf(String::isNotBlank) ?: session.accountLabel.value,
                message = "${draft.subject}|${draft.message}",
                dialog = GROUP_NOTICE_DIALOG,
                offline = ONLINE,
                instantMessageId = LibomvUuidCodec.xor(targetGroupId, agentId)
                    ?: return LibomvNoticeMappingResult.Failure,
                parentEstateId = PARENT_ESTATE_ID,
                timestamp = TIMESTAMP,
                regionId = ZERO_ID,
                position = LibomvNoticePosition.ZERO,
                attachment = noticeAttachment,
                binaryBucket = bucket,
            ),
        )
    }

    private fun noticeAttachment(attachment: AttachmentRef, agentId: String): LibomvNoticeAttachment? {
        val itemId = LibomvUuidCodec.canonicalOrNull(attachment.attachmentId.value) ?: return null
        val ownerId = LibomvUuidCodec.canonicalOrNull(agentId) ?: return null
        return LibomvNoticeAttachment(
            itemId = itemId,
            ownerId = ownerId,
            kind = attachment.kind,
        )
    }

    private fun attachmentBucket(attachment: LibomvNoticeAttachment): ByteArray = buildString {
        append("<llsd><map>")
        append("<key>item_id</key><uuid>").append(attachment.itemId).append("</uuid>")
        append("<key>owner_id</key><uuid>").append(attachment.ownerId).append("</uuid>")
        append("</map></llsd>")
    }.encodeToByteArray()
}
