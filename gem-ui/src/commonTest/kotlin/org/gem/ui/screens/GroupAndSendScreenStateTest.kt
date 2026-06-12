package org.gem.ui.screens

import org.gem.ui.state.GroupTargetMode
import org.gem.ui.state.GroupTargetUiState
import org.gem.ui.state.SendFooterUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupAndSendScreenStateTest {
    @Test
    fun groupAndSendSurfacesUseCanonicalTextKeys() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("Groups", catalogue.text(GemTextKey.Groups))
        assertEquals("Add all", catalogue.text(GemTextKey.AddAll))
        assertEquals("Select...", catalogue.text(GemTextKey.AddGroups))
        assertEquals("Can send notices", catalogue.text(GemTextKey.CanSendNotices))
        assertEquals("Send notices", catalogue.text(GemTextKey.SendNotices))
        assertEquals("Ready", catalogue.text(GemTextKey.Ready))
        assertEquals("Subject required", catalogue.text(GemTextKey.MissingSubject))
        assertEquals("Body required", catalogue.text(GemTextKey.MissingBody))
        assertEquals("Select groups", catalogue.text(GemTextKey.MissingGroups))
        assertEquals("Loading groups", catalogue.text(GemTextKey.LoadingGroups))
        assertEquals("No groups", catalogue.text(GemTextKey.GroupsEmpty))
        assertEquals("Groups unavailable", catalogue.text(GemTextKey.GroupsUnavailable))
        assertEquals("0 selected", catalogue.text(GemTextKey.SelectedCount(0)))
        assertEquals("1 selected", catalogue.text(GemTextKey.SelectedCount(1)))
        assertEquals("2 selected", catalogue.text(GemTextKey.SelectedCount(2)))
    }

    @Test
    fun groupAndFooterStateStartInPrototypeSafeShape() {
        val groups = GroupTargetUiState()
        val footer = SendFooterUiState()

        assertEquals(GroupTargetMode.NONE, groups.mode)
        assertEquals(0, groups.selectedCount)
        assertFalse(groups.pickerVisible)
        assertTrue(groups.loading)
        assertFalse(footer.enabled)
        assertEquals(GemTextKey.BlankStatus, footer.statusTextKey)
        assertTrue(footer.visible)
    }

    @Test
    fun groupAndSendTagsMapPrototypeHooks() {
        assertEquals("data-all-groups", GemTestTags.AllGroups)
        assertEquals("data-manual-groups", GemTestTags.ManualGroups)
        assertEquals("data-group-picker", GemTestTags.GroupPicker)
        assertEquals("data-group-list", GemTestTags.GroupList)
        assertEquals("data-group-count", GemTestTags.GroupCount)
        assertEquals("data-send-bar", GemTestTags.SendBar)
        assertEquals("data-status-text", GemTestTags.StatusText)
        assertEquals("data-primary-action", GemTestTags.PrimaryAction)
    }
}
