package org.hostess.core.domain

data class NoticeRecipientEstimate(
    val displayName: GroupDisplayName,
    val recipientCount: NoticeRecipientCount,
    val source: NoticeRecipientEstimateSource,
)
