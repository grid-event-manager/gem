package org.gem.ui.screens

import org.gem.ui.state.InventoryBrowserUiState
import org.gem.ui.state.NoticeComposerUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeScreenStateTest {
    @Test
    fun composeScreenUsesCanonicalEditorAndInventoryTextKeys() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("Subject", catalogue.text(GemTextKey.Subject))
        assertEquals("Body", catalogue.text(GemTextKey.Body))
        assertEquals("Inventory", catalogue.text(GemTextKey.Inventory))
        assertEquals("Landmarks", catalogue.text(GemTextKey.Landmarks))
        assertEquals("Textures", catalogue.text(GemTextKey.Textures))
        assertEquals("None", catalogue.text(GemTextKey.None))
        assertEquals("Loading inventory folders", catalogue.text(GemTextKey.LoadingInventory))
        assertEquals("No inventory", catalogue.text(GemTextKey.InventoryEmpty))
        assertEquals("Inventory unavailable", catalogue.text(GemTextKey.InventoryUnavailable))
        assertEquals("Loading groups", catalogue.text(GemTextKey.LoadingGroups))
        assertEquals("No groups", catalogue.text(GemTextKey.GroupsEmpty))
        assertEquals("Groups unavailable", catalogue.text(GemTextKey.GroupsUnavailable))
    }

    @Test
    fun composeScreenInitialStateLeavesGroupAndSendDisabledForNextSlice() {
        val notice = NoticeComposerUiState()
        val inventory = InventoryBrowserUiState()

        assertEquals(0, notice.charCount)
        assertEquals(GemTextKey.SelectedCount(0), notice.selectedTargetSummary)
        assertEquals(GemTextKey.None, notice.selectedAttachmentSummary)
        assertEquals(GemTextKey.None, inventory.attachmentSummary)
        assertEquals(true, inventory.loading)
        assertEquals(false, notice.sendFooterState.enabled)
        assertEquals("No attachments added", EnglishGemTextCatalogue.text(GemTextKey.NoAttachmentsAdded))
    }

    @Test
    fun composeScreenTagsMapPrototypeHooks() {
        assertEquals("data-view-compose", GemTestTags.ViewCompose)
        assertEquals("data-subject", GemTestTags.Subject)
        assertEquals("data-body", GemTestTags.Body)
        assertEquals("data-draft-count", GemTestTags.DraftCount)
        assertEquals("data-inventory-target", GemTestTags.InventoryTarget)
        assertEquals("data-inventory-path", GemTestTags.InventoryPath)
        assertEquals("data-inventory-list", GemTestTags.InventoryList)
        assertEquals("data-attachment-summary", GemTestTags.AttachmentSummary)
        assertEquals("data-clear-attachment", GemTestTags.ClearAttachment)
    }
}
