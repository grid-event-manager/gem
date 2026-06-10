package org.hostess.ui.screens

import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.NoticeComposerUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextKey
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeScreenStateTest {
    @Test
    fun composeScreenUsesCanonicalEditorAndInventoryTextKeys() {
        val catalogue = EnglishHostessTextCatalogue

        assertEquals("Notice", catalogue.text(HostessTextKey.Notice))
        assertEquals("Subject", catalogue.text(HostessTextKey.Subject))
        assertEquals("Body", catalogue.text(HostessTextKey.Body))
        assertEquals("Inventory", catalogue.text(HostessTextKey.Inventory))
        assertEquals("Folder", catalogue.text(HostessTextKey.Folder))
        assertEquals("Landmarks", catalogue.text(HostessTextKey.Landmarks))
        assertEquals("Textures", catalogue.text(HostessTextKey.Textures))
        assertEquals("Selected", catalogue.text(HostessTextKey.Selected))
        assertEquals("Select", catalogue.text(HostessTextKey.Select))
        assertEquals("Open", catalogue.text(HostessTextKey.Open))
        assertEquals("Landmark", catalogue.text(HostessTextKey.Landmark))
        assertEquals("Texture", catalogue.text(HostessTextKey.Texture))
        assertEquals("None", catalogue.text(HostessTextKey.None))
    }

    @Test
    fun composeScreenInitialStateLeavesGroupAndSendDisabledForNextSlice() {
        val notice = NoticeComposerUiState()
        val inventory = InventoryBrowserUiState()

        assertEquals(0, notice.charCount)
        assertEquals(HostessTextKey.SelectedCount(0), notice.selectedTargetSummary)
        assertEquals(HostessTextKey.None, notice.selectedAttachmentSummary)
        assertEquals(HostessTextKey.None, inventory.attachmentSummary)
        assertEquals(false, notice.sendFooterState.enabled)
    }

    @Test
    fun composeScreenTagsMapPrototypeHooks() {
        assertEquals("data-view-compose", HostessTestTags.ViewCompose)
        assertEquals("data-subject", HostessTestTags.Subject)
        assertEquals("data-body", HostessTestTags.Body)
        assertEquals("data-draft-count", HostessTestTags.DraftCount)
        assertEquals("data-inventory-target", HostessTestTags.InventoryTarget)
        assertEquals("data-inventory-path", HostessTestTags.InventoryPath)
        assertEquals("data-inventory-list", HostessTestTags.InventoryList)
        assertEquals("data-attachment-summary", HostessTestTags.AttachmentSummary)
    }
}
