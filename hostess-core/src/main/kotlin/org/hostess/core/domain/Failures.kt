package org.hostess.core.domain

data class CoreFailure(
    val reason: CoreFailureReason,
    val redactedMessage: String? = null,
)

enum class CoreFailureReason {
    LOGIN_FAILED,
    LOGOUT_FAILED,
    GROUP_LIST_FAILED,
    ATTACHMENT_NOT_FOUND,
    ATTACHMENT_CREATE_FAILED,
    ATTACHMENT_UPLOAD_FAILED,
    NOTICE_SEND_FAILED,
}
