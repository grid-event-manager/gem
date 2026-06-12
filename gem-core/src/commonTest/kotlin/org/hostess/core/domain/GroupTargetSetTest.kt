package org.hostess.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GroupTargetSetTest {
    @Test
    fun `adds and removes groups by display name`() {
        val targets = GroupTargetSet.from(listOf(group("music", "Music Room")))

        val added = assertIs<TargetSelectionResult.Changed>(targets.add(GroupDisplayName("Music Room"))).targetSet
        assertTrue(added.isSelected(GroupId("music")))
        assertEquals(listOf("Music Room"), added.selectedGroups.map { it.displayName.value })

        val removed = assertIs<TargetSelectionResult.Changed>(added.remove(GroupDisplayName("Music Room"))).targetSet
        assertFalse(removed.isSelected(GroupId("music")))
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `returns ambiguity when display name matches more than one group`() {
        val targets = GroupTargetSet.from(
            listOf(
                group("music-a", "Music Room"),
                group("music-b", "Music Room"),
            ),
        )

        val result = assertIs<TargetSelectionResult.AmbiguousDisplayName>(
            targets.add(GroupDisplayName("Music Room")),
        )

        assertEquals(2, result.matches.size)
        assertTrue(result.targetSet.isEmpty())
    }

    @Test
    fun `add all selects sendable groups and remove all clears them`() {
        val targets = GroupTargetSet.from(
            listOf(
                group("music", "Music Room"),
                group("gallery", "Gallery"),
                group("silent", "Silent Group", canSendNotices = false),
            ),
        )

        val selected = assertIs<TargetSelectionResult.Changed>(targets.addAllSendable()).targetSet
        assertEquals(listOf("Music Room", "Gallery"), selected.selectedGroups.map { it.displayName.value })
        assertFalse(selected.isSelected(GroupId("silent")))

        val cleared = assertIs<TargetSelectionResult.Changed>(selected.removeAll()).targetSet
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun `reports unknown and unsendable display-name selections`() {
        val targets = GroupTargetSet.from(
            listOf(group("silent", "Silent Group", canSendNotices = false)),
        )

        assertIs<TargetSelectionResult.NoSuchGroup>(targets.add(GroupDisplayName("Missing Group")))
        assertIs<TargetSelectionResult.CannotSendNotice>(targets.add(GroupDisplayName("Silent Group")))
    }

    @Test
    fun `reports unchanged for repeated add or remove`() {
        val targets = GroupTargetSet.from(listOf(group("music", "Music Room")))
        val selected = assertIs<TargetSelectionResult.Changed>(targets.add(GroupDisplayName("Music Room"))).targetSet

        assertIs<TargetSelectionResult.Unchanged>(selected.add(GroupDisplayName("Music Room")))
        assertIs<TargetSelectionResult.Unchanged>(targets.remove(GroupDisplayName("Music Room")))
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
