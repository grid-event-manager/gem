package org.gem.core.services

import org.gem.core.domain.AttachmentRequest
import org.gem.core.domain.GroupTargetSet
import org.gem.core.domain.NoticeDraft
import org.gem.core.domain.NoticeDraftValidation

class NoticeDraftService {
    fun createDraft(
        subject: String,
        message: String,
        targetSet: GroupTargetSet,
        attachments: List<AttachmentRequest> = emptyList(),
    ): NoticeDraft = NoticeDraft(subject, message, targetSet, attachments)

    fun validateForSend(draft: NoticeDraft): NoticeDraftValidation = draft.validateForSend()
}
