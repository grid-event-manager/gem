package org.hostess.core.services

import org.hostess.core.domain.GroupDisplayName
import org.hostess.core.domain.GroupId
import org.hostess.core.domain.GroupMembership
import org.hostess.core.domain.TargetSelectionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TargetSelectionServiceTest {
    private val service = TargetSelectionService()

    @Test
    fun `add and remove delegate to domain target rules`() {
        val targetSet = service.emptyTargetSet(listOf(group("music", "Music Room")))

        val selected = assertIs<TargetSelectionResult.Changed>(
            service.addTarget(targetSet, GroupDisplayName("Music Room")),
        ).targetSet
        assertTrue(selected.isSelected(GroupId("music")))

        val removed = assertIs<TargetSelectionResult.Changed>(
            service.removeTarget(selected, GroupDisplayName("Music Room")),
        ).targetSet
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `add all and remove all delegate to domain target rules`() {
        val targetSet = service.emptyTargetSet(
            listOf(
                group("music", "Music Room"),
                group("gallery", "Gallery"),
                group("silent", "Silent Group", canSendNotices = false),
            ),
        )

        val selected = assertIs<TargetSelectionResult.Changed>(service.addAllSendable(targetSet)).targetSet
        assertEquals(listOf("Music Room", "Gallery"), selected.selectedGroups.map { it.displayName.value })
        assertFalse(selected.isSelected(GroupId("silent")))

        val cleared = assertIs<TargetSelectionResult.Changed>(service.removeAll(selected)).targetSet
        assertTrue(cleared.isEmpty())
    }

    private fun group(
        id: String,
        displayName: String,
        canSendNotices: Boolean = true,
    ): GroupMembership = GroupMembership(
        groupId = GroupId(id),
        displayName = GroupDisplayName(displayName),
        canSendNotices = canSendNotices,
        acceptsNotices = null,
    )
}
