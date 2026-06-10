package org.hostess.ui.controllers

import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.testing.FakeHostessUiRuntime
import org.hostess.ui.testing.FakeInventoryFixtures
import org.hostess.ui.text.HostessTextKey
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
        assertEquals(HostessTextKey.None, controller.state.attachmentSummary)
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
        val refreshed = controllerForFixture().refreshInventory()
        val landmark = refreshed.selectInventoryAsset(fixture.welcomeItemId)
        val texture = refreshed
            .openInventoryShortcut(InventoryShortcut.TEXTURES)
            .selectInventoryAsset(fixture.textureItemId)

        assertEquals(InventoryItemKind.LANDMARK, landmark.state.selectedAttachment?.kind)
        assertEquals(fixture.welcomeItemId, landmark.state.selectedAttachment?.attachmentRef?.attachmentId)
        assertEquals(HostessTextKey.SelectedCount(1), landmark.state.attachmentSummary)
        assertTrue(landmark.state.visibleAssetRows.first { it.itemId == fixture.welcomeItemId }.selected)
        assertEquals(InventoryItemKind.TEXTURE, texture.state.selectedAttachment?.kind)
        assertEquals(fixture.textureItemId, texture.state.selectedAttachment?.attachmentRef?.attachmentId)
        assertEquals(HostessTextKey.SelectedCount(1), texture.state.attachmentSummary)
    }

    @Test
    fun missingLandmarksFolderDefaultsToRootWithErrorState() {
        val fixture = FakeInventoryFixtures
        val runtime = FakeHostessUiRuntime.ready(inventoryListing = fixture.listingWithoutLandmarks())
        val controller = InventoryBrowserController(runtime, fixture.session()).refreshInventory()

        assertEquals(fixture.rootFolderId, controller.state.currentFolderId)
        assertTrue(controller.state.shortcuts.rootSelected)
        assertEquals(HostessTextKey.InventoryUnavailable, controller.state.errorKey)
        assertFalse(controller.state.loading)
    }

    @Test
    fun unknownFolderOrAssetDoesNotCreateSelection() {
        val controller = controllerForFixture().refreshInventory()
        val unknownFolder = controller.openInventoryFolder(FakeInventoryFixtures.texturesFolderId)
            .openInventoryFolder(FakeInventoryFixtures.landmarksFolderId)
            .openInventoryFolder(org.hostess.core.domain.InventoryFolderId("folder:missing"))
        val unknownAsset = controller.selectInventoryAsset(InventoryItemId("item:missing"))

        assertEquals(HostessTextKey.InventoryUnavailable, unknownFolder.state.errorKey)
        assertEquals(HostessTextKey.InventoryUnavailable, unknownAsset.state.errorKey)
        assertNull(unknownAsset.state.selectedAttachment)
        assertEquals(HostessTextKey.None, unknownAsset.state.attachmentSummary)
    }

    @Test
    fun noCopyOrAttachmentFailureClearsSelectionAndReportsError() {
        val fixture = FakeInventoryFixtures
        val noCopy = controllerForFixture().refreshInventory().selectInventoryAsset(fixture.noCopyItemId)
        val failingRuntime = FakeHostessUiRuntime.ready(
            inventoryListing = fixture.listing(),
            attachmentSucceeds = false,
        )
        val attachmentFailure = InventoryBrowserController(failingRuntime, fixture.session())
            .refreshInventory()
            .selectInventoryAsset(fixture.welcomeItemId)

        assertEquals(HostessTextKey.InventoryUnavailable, noCopy.state.errorKey)
        assertNull(noCopy.state.selectedAttachment)
        assertEquals(HostessTextKey.None, noCopy.state.attachmentSummary)
        assertEquals(HostessTextKey.InventoryUnavailable, attachmentFailure.state.errorKey)
        assertNull(attachmentFailure.state.selectedAttachment)
    }

    @Test
    fun loadingEmptyAndFailureStatesAreExplicitForVisiblePaneProjection() {
        val initial = InventoryBrowserController(FakeHostessUiRuntime.ready(), FakeInventoryFixtures.session()).state
        val empty = InventoryBrowserController(
            FakeHostessUiRuntime.ready(inventoryListing = FakeInventoryFixtures.listingWithEmptyLandmarks()),
            FakeInventoryFixtures.session(),
        )
            .refreshInventory()
        val failed = InventoryBrowserController(
            FakeHostessUiRuntime.ready(inventoryListSucceeds = false),
            FakeInventoryFixtures.session(),
        ).refreshInventory()

        assertTrue(initial.loading)
        assertFalse(empty.state.loading)
        assertTrue(empty.state.visibleFolderRows.isEmpty())
        assertTrue(empty.state.visibleAssetRows.isEmpty())
        assertNull(empty.state.errorKey)
        assertFalse(failed.state.loading)
        assertEquals(HostessTextKey.InventoryUnavailable, failed.state.errorKey)
    }

    private fun controllerForFixture(): InventoryBrowserController {
        val fixture = FakeInventoryFixtures
        val runtime = FakeHostessUiRuntime.ready(inventoryListing = fixture.listing())
        return InventoryBrowserController(runtime, fixture.session())
    }
}
