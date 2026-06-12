package org.hostess.ui.controllers

import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.state.GroupTargetMode
import org.hostess.ui.state.InventoryAssetRowUiState
import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.state.UiRoute
import org.hostess.ui.testing.FakeGroupFixtures
import org.hostess.ui.testing.FakeInventoryFixtures
import org.hostess.ui.testing.FakeHostessUiRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ControllerEntrypointShapeTest {
    private val runtime = FakeHostessUiRuntime.ready()

    @Test
    fun appControllerOwnsRouteMenuAndLogoutShape() {
        val opened = HostessAppController(runtime).openMenu()
        val settings = opened.openSettings()
        val loggedOut = settings.logout()

        assertTrue(opened.state.menuOpen)
        assertEquals(UiRoute.Settings, settings.state.route)
        assertFalse(settings.state.menuOpen)
        assertEquals(UiRoute.Login, loggedOut.state.route)
        assertNull(loggedOut.state.session)
    }

    @Test
    fun loginControllerOwnsSavedLoginAndPanelEntryPoints() {
        val profileId = FakeHostessUiRuntime.defaultProfile().profileId
        val controller = LoginController(runtime)
            .refreshSavedLogins()
            .selectSavedLogin(profileId)
            .updatePasswordDraft("changed-password")
            .togglePasswordVisibility()
        val loggedIn = controller.beginLogin().completeLogin()
        val savedNew = controller
            .updateUsernameDraft("newavatar")
            .normalizeLoginNameOnPasswordFocus()
            .updatePasswordDraft("new-password")
            .beginLogin()
            .completeLogin()

        assertEquals(profileId, controller.state.selectedProfileId)
        assertEquals("changed-password", controller.state.passwordDraft)
        assertTrue(controller.state.passwordVisible)
        assertTrue(controller.state.passwordEnabled)
        assertTrue(controller.state.loginEnabled)
        assertEquals(UiRoute.Compose, loggedIn.appState.route)
        assertEquals(UiRoute.Compose, savedNew.appState.route)
    }

    @Test
    fun settingsControllerOwnsPasswordDeleteAndAccountEntryPoints() {
        val profileId = FakeHostessUiRuntime.defaultProfile().profileId
        val selected = SettingsController(runtime)
            .refreshSavedAccounts()
            .selectSavedAccount(profileId)
            .toggleEditAccountPanel()
            .updateSavedPasswordDraft("settings-password")
            .saveEditedPassword()
            .toggleEditAccountPanel()
            .toggleSavedPasswordVisibility()
            .toggleAddAccountPanel()
            .updateNewUsernameDraft("settingsavatar")
            .normalizeNewAccountNameOnPasswordFocus()
            .openDeleteAccounts()
            .setDeleteAccountSelected(profileId, true)
        val modalOpen = selected.openDeleteAccounts()
        val controller = modalOpen
            .cancelDeleteAccounts()
        val savedNew = selected
            .updateNewPasswordDraft("new-password")
            .saveNewAccount()

        assertEquals(profileId, controller.state.selectedProfileId)
        assertEquals("settings-password", controller.state.passwordDraft)
        assertTrue(controller.state.passwordVisible)
        assertTrue(controller.state.passwordEnabled)
        assertTrue(controller.state.editAccountExpanded)
        assertTrue(controller.state.addAccountExpanded)
        assertTrue(controller.state.deleteExpanded)
        assertEquals(setOf(profileId), controller.state.selectedDeleteProfileIds)
        assertEquals("settingsavatar resident", controller.state.addUsernameDraft)
        assertTrue(savedNew.state.savedLoginOptions.any { it.loginName == "settingsavatar resident" })
        assertTrue(modalOpen.state.confirmDeleteOpen)
        assertFalse(controller.state.confirmDeleteOpen)
    }

    @Test
    fun noticeComposerControllerOwnsDraftEntryPointsWithoutSending() {
        val controller = NoticeComposerController(runtime)
            .updateSubject("Event tonight")
            .updateBody("Body with emoji \uD83C\uDFA7")
            .sendNotices()

        assertEquals("Event tonight", controller.state.subject)
        assertEquals("Body with emoji \uD83C\uDFA7", controller.state.body)
        assertEquals(controller.state.body.length, controller.state.charCount)
    }

    @Test
    fun groupControllerOwnsModeAndManualRowProjection() {
        val groupRuntime = FakeHostessUiRuntime.ready(
            groups = listOf(FakeGroupFixtures.owks),
        )
        val refreshed = NoticeComposerController(
            runtime = groupRuntime,
            session = FakeInventoryFixtures.session(),
            avatarReady = true,
        ).refreshGroups()

        val all = refreshed.selectAllGroupsMode()
        val manual = all.selectManualGroupsMode().setManualGroupSelected("Owks", true)

        assertEquals(GroupTargetMode.ALL, all.state.mode)
        assertFalse(all.state.pickerVisible)
        assertEquals(GroupTargetMode.MANUAL, manual.state.mode)
        assertTrue(manual.state.pickerVisible)
        assertEquals(1, manual.state.selectedCount)
        assertTrue(manual.state.rows.single().selected)
    }

    @Test
    fun inventoryControllerOwnsShortcutFolderAndAssetProjection() {
        val fixture = FakeInventoryFixtures
        val inventoryRuntime = FakeHostessUiRuntime.ready(
            inventoryListing = fixture.listing(),
        )
        val refreshed = InventoryBrowserController(inventoryRuntime, fixture.session()).refreshInventory()

        val shortcut = refreshed
            .openInventoryShortcut(InventoryShortcut.TEXTURES)
            .selectInventoryAsset(fixture.textureItemId)
        val folder = refreshed.openInventoryFolder(fixture.venuesFolderId)

        assertTrue(shortcut.state.shortcuts.texturesSelected)
        assertTrue(shortcut.state.visibleAssetRows.single().selected)
        assertEquals(fixture.venuesFolderId, folder.state.currentFolderId)
    }
}
