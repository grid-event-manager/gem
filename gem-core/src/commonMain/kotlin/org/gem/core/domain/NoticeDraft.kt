package org.gem.core.domain

class NoticeDraft(
    val subject: String,
    val message: String,
    val targetSet: GroupTargetSet,
    attachments: List<AttachmentRequest> = emptyList(),
) {
    val attachments: List<AttachmentRequest> = attachments.toList()

    fun validateForSend(): NoticeDraftValidation {
        val reasons = buildSet {
            if (subject.isBlank()) {
                add(NoticeDraftInvalidReason.BLANK_SUBJECT)
            }
            if (message.isBlank()) {
                add(NoticeDraftInvalidReason.BLANK_MESSAGE)
            }
            if (targetSet.isEmpty()) {
                add(NoticeDraftInvalidReason.EMPTY_TARGET_SET)
            }
            if (attachments.size > 1) {
                add(NoticeDraftInvalidReason.TOO_MANY_ATTACHMENTS)
            }
        }

        return if (reasons.isEmpty()) {
            NoticeDraftValidation.Valid
        } else {
            NoticeDraftValidation.Invalid(reasons)
        }
    }

    fun withAttachment(attachment: AttachmentRequest): NoticeDraft = copy(attachments = listOf(attachment))

    fun withoutAttachment(): NoticeDraft = copy(attachments = emptyList())

    fun copy(
        subject: String = this.subject,
        message: String = this.message,
        targetSet: GroupTargetSet = this.targetSet,
        attachments: List<AttachmentRequest> = this.attachments,
    ): NoticeDraft = NoticeDraft(subject, message, targetSet, attachments)
}

sealed interface NoticeDraftValidation {
    data object Valid : NoticeDraftValidation
    data class Invalid(val reasons: Set<NoticeDraftInvalidReason>) : NoticeDraftValidation
}

enum class NoticeDraftInvalidReason {
    BLANK_SUBJECT,
    BLANK_MESSAGE,
    EMPTY_TARGET_SET,
    TOO_MANY_ATTACHMENTS,
}
