package org.hostess.core.domain

data class NoticeDeliveryProjection(
    val selectedGroups: List<GroupMembership>,
    val projectedDeliveryCount: NoticeDeliveryCount,
    val estimates: List<NoticeRecipientEstimate>,
) {
    init {
        require(selectedGroups.isNotEmpty()) { "NoticeDeliveryProjection requires selected groups." }
        require(estimates.size == selectedGroups.size) {
            "NoticeDeliveryProjection requires one estimate per selected group."
        }
    }
}
