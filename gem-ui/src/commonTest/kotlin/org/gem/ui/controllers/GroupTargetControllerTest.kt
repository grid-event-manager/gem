package org.gem.ui.controllers

import org.gem.core.domain.GroupMembership
import org.gem.ui.state.GroupTargetMode
import org.gem.ui.testing.FakeGroupFixtures
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeInventoryFixtures
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupTargetControllerTest {
    @Test
    fun refreshGroupsStartsWithNoSelectionAndCollapsedPicker() {
        val controller = refreshedController(FakeGroupFixtures.mixedGroups())

        assertEquals(GroupTargetMode.NONE, controller.state.mode)
        assertEquals(0, controller.state.selectedCount)
        assertFalse(controller.state.pickerVisible)
        assertFalse(controller.state.loading)
        assertEquals(2, controller.state.rows.size)
        assertFalse(controller.state.rows.any { it.displayName == "Audience" })
        assertTrue(controller.state.rows.none { it.selected })
    }

    @Test
    fun addAllSelectsOnlySendableGroupsAndDeselectsManualMode() {
        val controller = refreshedController(FakeGroupFixtures.mixedGroups())
            .selectManualGroupsMode()
            .setManualGroupSelected("Owks", true)
            .selectAllGroupsMode()

        assertEquals(GroupTargetMode.ALL, controller.state.mode)
        assertFalse(controller.state.pickerVisible)
        assertEquals(2, controller.state.selectedCount)
        assertTrue(controller.state.rows.first { it.displayName == "Owks" }.selected)
        assertTrue(controller.state.rows.first { it.displayName == "m!nx" }.selected)
        assertFalse(controller.state.rows.any { it.displayName == "Audience" })
    }

    @Test
    fun manualModeFromAllClearsSelectionAndExpandsPicker() {
        val controller = refreshedController(FakeGroupFixtures.mixedGroups())
            .selectAllGroupsMode()
            .selectManualGroupsMode()

        assertEquals(GroupTargetMode.MANUAL, controller.state.mode)
        assertTrue(controller.state.pickerVisible)
        assertEquals(0, controller.state.selectedCount)
        assertTrue(controller.state.rows.none { it.selected })
    }

    @Test
    fun manualRowsToggleByDisplayNameThroughTargetSelectionService() {
        val selected = refreshedController(FakeGroupFixtures.mixedGroups())
            .selectManualGroupsMode()
            .setManualGroupSelected("Owks", true)
        val removed = selected.setManualGroupSelected("Owks", false)

        assertEquals(GroupTargetMode.MANUAL, selected.state.mode)
        assertEquals(1, selected.state.selectedCount)
        assertTrue(selected.state.rows.first { it.displayName == "Owks" }.selected)
        assertEquals(0, removed.state.selectedCount)
        assertFalse(removed.state.rows.first { it.displayName == "Owks" }.selected)
    }

    @Test
    fun redPathsDoNotSelectUnknownDuplicateOrNonSendableGroups() {
        val nonSendable = refreshedController(FakeGroupFixtures.mixedGroups())
            .selectManualGroupsMode()
            .setManualGroupSelected("Audience", true)
        val unknown = refreshedController(FakeGroupFixtures.mixedGroups())
            .selectManualGroupsMode()
            .setManualGroupSelected("Missing", true)
        val duplicate = refreshedController(FakeGroupFixtures.duplicateGroups())
            .selectManualGroupsMode()
            .setManualGroupSelected("Duplicate", true)

        assertEquals(GemTextKey.BlankStatus, nonSendable.state.errorKey)
        assertEquals(GemTextKey.BlankStatus, unknown.state.errorKey)
        assertEquals(GemTextKey.BlankStatus, duplicate.state.errorKey)
        assertEquals(0, nonSendable.state.selectedCount)
        assertEquals(0, unknown.state.selectedCount)
        assertEquals(0, duplicate.state.selectedCount)
    }

    @Test
    fun hiddenNonSendableDuplicateDoesNotBlockVisibleSendableGroupSelection() {
        val groups = listOf(
            group("venue-send", "Venue Notices", canSendNotices = true),
            group("venue-read", "Venue Notices", canSendNotices = false),
        )

        val selected = refreshedController(groups)
            .selectManualGroupsMode()
            .setManualGroupSelected("Venue Notices", true)

        assertEquals(1, selected.state.rows.size)
        assertEquals(1, selected.state.selectedCount)
        assertTrue(selected.state.rows.single().selected)
    }

    @Test
    fun addAllWithNoSendableGroupsLeavesTargetsEmpty() {
        val controller = refreshedController(listOf(FakeGroupFixtures.audience))
            .selectAllGroupsMode()

        assertEquals(GroupTargetMode.ALL, controller.state.mode)
        assertEquals(0, controller.state.selectedCount)
        assertTrue(controller.state.rows.isEmpty())
    }

    @Test
    fun loadingEmptyAndFailureStatesAreExplicitForVisiblePickerProjection() {
        val initial = GroupTargetController(FakeGemUiRuntime.ready()).state
        val empty = refreshedController(emptyList())
        val failed = NoticeComposerController(
            runtime = FakeGemUiRuntime.ready(groupListSucceeds = false),
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        ).refreshGroups()

        assertTrue(initial.loading)
        assertFalse(empty.state.loading)
        assertTrue(empty.state.rows.isEmpty())
        assertEquals(null, empty.state.errorKey)
        assertFalse(failed.state.loading)
        assertEquals(GemTextKey.GroupsUnavailable, failed.state.errorKey)
    }

    private fun refreshedController(groups: List<GroupMembership>): GroupTargetController {
        val runtime = FakeGemUiRuntime.ready(groups = groups)
        return NoticeComposerController(
            runtime = runtime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        ).refreshGroups()
    }

    private fun group(
        id: String,
        displayName: String,
        canSendNotices: Boolean,
    ): GroupMembership =
        GroupMembership.fromValues(
            groupId = id,
            displayName = displayName,
            canSendNotices = canSendNotices,
            acceptsNotices = true,
        )
}
