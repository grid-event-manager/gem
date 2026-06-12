package org.gem.core.domain

data class CoreFailure(
    val reason: CoreFailureReason,
    val redactedMessage: String? = null,
)

enum class CoreFailureReason {
    LOGIN_FAILED,
    LOGOUT_FAILED,
    GROUP_LIST_FAILED,
    INVENTORY_LIST_FAILED,
    ATTACHMENT_NOT_FOUND,
    NOTICE_SEND_FAILED,
    AVATAR_READINESS_FAILED,
}
