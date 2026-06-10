package org.hostess.ui.screens

import org.hostess.ui.state.GroupTargetMode
import org.hostess.ui.state.GroupTargetUiState
import org.hostess.ui.state.SendFooterUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupAndSendScreenStateTest {
    @Test
    fun groupAndSendSurfacesUseCanonicalTextKeys() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("Groups", catalogue.text(HostessTextKey.Groups))
        assertEquals("Add all", catalogue.text(HostessTextKey.AddAll))
        assertEquals("Add Groups", catalogue.text(HostessTextKey.AddGroups))
        assertEquals("Can send notices", catalogue.text(HostessTextKey.CanSendNotices))
        assertEquals("Send notices", catalogue.text(HostessTextKey.SendNotices))
        assertEquals("Ready", catalogue.text(HostessTextKey.Ready))
        assertEquals("Sending", catalogue.text(HostessTextKey.Sending))
        assertEquals("Loading groups", catalogue.text(HostessTextKey.LoadingGroups))
        assertEquals("No groups", catalogue.text(HostessTextKey.GroupsEmpty))
        assertEquals("Groups unavailable", catalogue.text(HostessTextKey.GroupsUnavailable))
        assertEquals("0 selected", catalogue.text(HostessTextKey.SelectedCount(0)))
        assertEquals("1 selected", catalogue.text(HostessTextKey.SelectedCount(1)))
        assertEquals("2 selected", catalogue.text(HostessTextKey.SelectedCount(2)))
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
        assertEquals(HostessTextKey.BlankStatus, footer.statusTextKey)
        assertTrue(footer.visible)
    }

    @Test
    fun groupAndSendTagsMapPrototypeHooks() {
        assertEquals("data-all-groups", HostessTestTags.AllGroups)
        assertEquals("data-manual-groups", HostessTestTags.ManualGroups)
        assertEquals("data-group-picker", HostessTestTags.GroupPicker)
        assertEquals("data-group-list", HostessTestTags.GroupList)
        assertEquals("data-group-count", HostessTestTags.GroupCount)
        assertEquals("data-send-bar", HostessTestTags.SendBar)
        assertEquals("data-status-text", HostessTestTags.StatusText)
        assertEquals("data-primary-action", HostessTestTags.PrimaryAction)
    }
}
