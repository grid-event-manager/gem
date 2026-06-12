package org.gem.ui.controllers

import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemKind
import org.gem.ui.state.InventoryShortcut
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testing.FakeInventoryFixtures
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InventoryBrowserControllerTest {
    @Test
    fun refreshDefaultsToLandmarksFolderAndProjectsChildFoldersAndAssets() {
        val fixture = FakeInventoryFixtures
        val controller = controllerForFixture().refreshInventory()

        assertEquals(fixture.landmarksFolderId, controller.state.currentFolderId)
        assertEquals(listOf("Inventory", "Landmarks"), controller.state.currentPath)
        assertTrue(controller.state.shortcuts.landmarksSelected)
        assertFalse(controller.state.shortcuts.rootSelected)
        assertEquals(listOf("Venues"), controller.state.visibleFolderRows.map { it.displayName })
        assertEquals(listOf("Welcome Area", "No Copy Landmark"), controller.state.visibleAssetRows.map { it.displayName })
        assertEquals(GemTextKey.None, controller.state.attachmentSummary)
        assertFalse(controller.state.loading)
        assertNull(controller.state.errorKey)
    }

    @Test
    fun shortcutsAndFolderOpenProjectPathAndRowsFromExistingListing() {
        val fixture = FakeInventoryFixtures
        val refreshed = controllerForFixture().refreshInventory()
        val root = refreshed.openInventoryShortcut(InventoryShortcut.ROOT)
        val textures = refreshed.openInventoryShortcut(InventoryShortcut.TEXTURES)
        val venueFolder = refreshed.openInventoryFolder(fixture.venuesFolderId)

        assertEquals(fixture.rootFolderId, root.state.currentFolderId)
        assertTrue(root.state.shortcuts.rootSelected)
        assertEquals(listOf("Landmarks", "Textures"), root.state.visibleFolderRows.map { it.displayName })
        assertEquals(fixture.texturesFolderId, textures.state.currentFolderId)
        assertTrue(textures.state.shortcuts.texturesSelected)
        assertEquals(listOf("Stage Poster"), textures.state.visibleAssetRows.map { it.displayName })
        assertEquals(listOf("Inventory", "Landmarks", "Venues"), venueFolder.state.currentPath)
        assertEquals(listOf("Venue Stage"), venueFolder.state.visibleAssetRows.map { it.displayName })
    }

    @Test
    fun selectingLandmarkOrTextureUsesCoreSelectionAndAttachmentResolution() {
        val fixture = FakeInventoryFixtures
        val refreshed = controllerForFixture(attachmentSucceeds = false).refreshInventory()
        val landmark = refreshed.selectInventoryAsset(fixture.welcomeItemId)
        val texture = refreshed
            .openInventoryShortcut(InventoryShortcut.TEXTURES)
            .selectInventoryAsset(fixture.textureItemId)

        assertEquals(InventoryItemKind.LANDMARK, landmark.state.selectedAttachment?.kind)
        assertEquals(fixture.welcomeItemId, landmark.state.selectedAttachment?.attachmentRef?.attachmentId)
        assertEquals("owner:item:welcome", landmark.state.selectedAttachment?.attachmentRef?.ownerId?.value)
        assertEquals(GemTextKey.SelectedCount(1), landmark.state.attachmentSummary)
        assertTrue(landmark.state.visibleAssetRows.first { it.itemId == fixture.welcomeItemId }.selected)
        assertEquals(InventoryItemKind.TEXTURE, texture.state.selectedAttachment?.kind)
        assertEquals(fixture.textureItemId, texture.state.selectedAttachment?.attachmentRef?.attachmentId)
        assertEquals("owner:item:texture", texture.state.selectedAttachment?.attachmentRef?.ownerId?.value)
        assertEquals(GemTextKey.SelectedCount(1), texture.state.attachmentSummary)
    }

    @Test
    fun clearingSelectedAttachmentResetsRowsAndSummary() {
        val fixture = FakeInventoryFixtures
        val selected = controllerForFixture()
            .refreshInventory()
            .selectInventoryAsset(fixture.welcomeItemId)

        val cleared = selected.clearSelectedAttachment()

        assertNull(cleared.state.selectedAttachment)
        assertEquals(GemTextKey.None, cleared.state.attachmentSummary)
        assertTrue(cleared.state.visibleAssetRows.none { it.selected })
        assertNull(cleared.state.errorKey)
    }

    @Test
    fun missingLandmarksFolderDefaultsToRootWithErrorState() {
        val fixture = FakeInventoryFixtures
        val runtime = FakeGemUiRuntime.ready(inventoryListing = fixture.listingWithoutLandmarks())
        val controller = InventoryBrowserController(runtime, fixture.session()).refreshInventory()

        assertEquals(fixture.rootFolderId, controller.state.currentFolderId)
        assertTrue(controller.state.shortcuts.rootSelected)
        assertEquals(GemTextKey.InventoryUnavailable, controller.state.errorKey)
        assertFalse(controller.state.loading)
    }

    @Test
    fun unknownFolderOrAssetDoesNotCreateSelection() {
        val controller = controllerForFixture().refreshInventory()
        val unknownFolder = controller.openInventoryFolder(FakeInventoryFixtures.texturesFolderId)
            .openInventoryFolder(FakeInventoryFixtures.landmarksFolderId)
            .openInventoryFolder(org.gem.core.domain.InventoryFolderId("folder:missing"))
        val unknownAsset = controller.selectInventoryAsset(InventoryItemId("item:missing"))

        assertEquals(GemTextKey.InventoryUnavailable, unknownFolder.state.errorKey)
        assertEquals(GemTextKey.InventoryUnavailable, unknownAsset.state.errorKey)
        assertNull(unknownAsset.state.selectedAttachment)
        assertEquals(GemTextKey.None, unknownAsset.state.attachmentSummary)
    }

    @Test
    fun noCopyOrAttachmentFailureClearsSelectionAndReportsError() {
        val fixture = FakeInventoryFixtures
        val noCopy = controllerForFixture().refreshInventory().selectInventoryAsset(fixture.noCopyItemId)
        val missingOwnerRuntime = FakeGemUiRuntime.ready(
            inventoryListing = fixture.listingWithoutAttachmentOwners(),
            attachmentSucceeds = false,
        )
        val attachmentFailure = InventoryBrowserController(missingOwnerRuntime, fixture.session())
            .refreshInventory()
            .selectInventoryAsset(fixture.welcomeItemId)

        assertEquals(GemTextKey.InventoryUnavailable, noCopy.state.errorKey)
        assertNull(noCopy.state.selectedAttachment)
        assertEquals(GemTextKey.None, noCopy.state.attachmentSummary)
        assertEquals(GemTextKey.InventoryUnavailable, attachmentFailure.state.errorKey)
        assertNull(attachmentFailure.state.selectedAttachment)
    }

    @Test
    fun loadingEmptyAndFailureStatesAreExplicitForVisiblePaneProjection() {
        val initial = InventoryBrowserController(FakeGemUiRuntime.ready(), FakeInventoryFixtures.session()).state
        val empty = InventoryBrowserController(
            FakeGemUiRuntime.ready(inventoryListing = FakeInventoryFixtures.listingWithEmptyLandmarks()),
            FakeInventoryFixtures.session(),
        )
            .refreshInventory()
        val failed = InventoryBrowserController(
            FakeGemUiRuntime.ready(inventoryListSucceeds = false),
            FakeInventoryFixtures.session(),
        ).refreshInventory()

        assertTrue(initial.loading)
        assertFalse(empty.state.loading)
        assertTrue(empty.state.visibleFolderRows.isEmpty())
        assertTrue(empty.state.visibleAssetRows.isEmpty())
        assertNull(empty.state.errorKey)
        assertFalse(failed.state.loading)
        assertEquals(GemTextKey.InventoryUnavailable, failed.state.errorKey)
    }

    private fun controllerForFixture(attachmentSucceeds: Boolean = true): InventoryBrowserController {
        val fixture = FakeInventoryFixtures
        val runtime = FakeGemUiRuntime.ready(
            inventoryListing = fixture.listing(),
            attachmentSucceeds = attachmentSucceeds,
        )
        return InventoryBrowserController(runtime, fixture.session())
    }
}
