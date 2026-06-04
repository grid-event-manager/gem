package org.hostess.core.services

import org.hostess.core.domain.AttachmentRequest
import org.hostess.core.domain.GroupTargetSet
import org.hostess.core.domain.NoticeDraft
import org.hostess.core.domain.NoticeDraftValidation

class NoticeDraftService {
    fun createDraft(
        subject: String,
        message: String,
        targetSet: GroupTargetSet,
        attachments: List<AttachmentRequest> = emptyList(),
    ): NoticeDraft = NoticeDraft(subject, message, targetSet, attachments)

    fun validateForSend(draft: NoticeDraft): NoticeDraftValidation = draft.validateForSend()
}
