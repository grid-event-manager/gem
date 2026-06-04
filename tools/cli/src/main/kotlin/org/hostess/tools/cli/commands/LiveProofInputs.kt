package org.hostess.tools.cli.commands

import org.hostess.core.domain.AttachmentKind
import org.hostess.core.domain.AttachmentRequest
import org.hostess.core.domain.CreateLandmarkAttachment
import org.hostess.core.domain.ExistingInventoryAttachment
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.LocalPosition
import org.hostess.core.domain.UploadTextureAttachment
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode

internal data class LiveProofInputs(
    val grid: String?,
    val account: String?,
    val credentialHandle: String?,
    val targetDisplayName: String?,
    val subject: String?,
    val body: String?,
    val authorisedLiveSend: Boolean,
    val attachmentKind: String?,
    val attachmentSource: String?,
    val attachmentDigest: String?,
) {
    fun missingRequiredFields(): List<String> = buildList {
        if (!authorisedLiveSend) add("authorised-live-send")
        if (grid.isNullOrBlank()) add("grid")
        if (account.isNullOrBlank()) add("account")
        if (credentialHandle.isNullOrBlank()) add("credential handle")
        if (targetDisplayName.isNullOrBlank()) add("target display name")
        if (subject.isNullOrBlank()) add("subject")
        if (body.isNullOrBlank()) add("body")
    }

    fun toReportInputs(mode: CommandMode): Map<String, String> = buildMap {
        put("mode", mode.label())
        put("grid", grid.orEmpty())
        put("account", account.orEmpty())
        put("credentialHandle", credentialHandle.orEmpty())
        put("targetDisplayName", targetDisplayName.orEmpty())
        put("subject", subject.orEmpty())
        put("bodyLength", body.orEmpty().length.toString())
        put("authorisedLiveSend", authorisedLiveSend.toString())
        attachmentKind?.let { put("attachmentKind", it) }
        attachmentSource?.let { put("attachmentSource", it) }
    }

    fun attachmentRequest(): AttachmentRequest? {
        val kind = attachmentKind?.lowercase() ?: return null
        val source = attachmentSource ?: return null
        return when (kind) {
            "landmark" -> CreateLandmarkAttachment(
                venueLabel = source,
                regionId = source,
                localPosition = LocalPosition(0.0, 0.0, 0.0),
            )
            "texture" -> UploadTextureAttachment(
                fileName = source,
                contentDigest = attachmentDigest ?: source,
            )
            "inventory-landmark" -> ExistingInventoryAttachment(AttachmentKind.LANDMARK, InventoryItemId(source))
            "inventory-texture" -> ExistingInventoryAttachment(AttachmentKind.TEXTURE, InventoryItemId(source))
            else -> null
        }
    }

    companion object {
        fun from(arguments: CommandArguments): LiveProofInputs = LiveProofInputs(
            grid = arguments.option("grid"),
            account = arguments.option("account"),
            credentialHandle = arguments.option("credential-env") ?: arguments.option("credential-file"),
            targetDisplayName = arguments.option("target") ?: arguments.option("group"),
            subject = arguments.option("subject"),
            body = arguments.option("body"),
            authorisedLiveSend = arguments.has("authorised-live-send"),
            attachmentKind = arguments.option("attachment-kind"),
            attachmentSource = arguments.option("attachment-source"),
            attachmentDigest = arguments.option("attachment-digest"),
        )
    }
}
