package org.hostess.ui.controllers

import org.hostess.core.domain.GroupId
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.core.domain.AccountProfileId
import org.hostess.ui.state.GroupTargetRowUiState
import org.hostess.ui.state.GroupTargetMode
import org.hostess.ui.state.GroupTargetUiState
import org.hostess.ui.state.InventoryAssetRowUiState
import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.state.UiRoute
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
            .updateSavedPasswordDraft("changed-password")
            .toggleSavedPasswordVisibility()
            .toggleAddLoginPanel()
            .updateNewUsernameDraft("newavatar")
            .normalizeNewLoginNameOnPasswordFocus()
        val loggedIn = controller.loginSelected()
        val savedNew = controller
            .updateNewPasswordDraft("new-password")
            .saveAndLogin()

        assertEquals(profileId, controller.state.selectedProfileId)
        assertEquals("changed-password", controller.state.passwordDraft)
        assertTrue(controller.state.passwordVisible)
        assertTrue(controller.state.passwordEnabled)
        assertTrue(controller.state.loginEnabled)
        assertTrue(controller.state.addLoginExpanded)
        assertEquals("newavatar resident", controller.state.newUsernameDraft)
        assertEquals(UiRoute.Compose, loggedIn.appState.route)
        assertEquals(UiRoute.Compose, savedNew.appState.route)
    }

    @Test
    fun settingsControllerOwnsPasswordDeleteAndAccountEntryPoints() {
        val profileId = FakeHostessUiRuntime.defaultProfile().profileId
        val selected = SettingsController(runtime)
            .refreshSavedAccounts()
            .selectSavedAccount(profileId)
            .updateSavedPasswordDraft("settings-password")
            .toggleSavedPasswordVisibility()
            .toggleAddAccountPanel()
            .updateNewUsernameDraft("settingsavatar")
            .normalizeNewAccountNameOnPasswordFocus()
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
        assertTrue(controller.state.addAccountExpanded)
        assertTrue(controller.state.deleteExpanded)
        assertEquals(setOf(profileId), controller.state.selectedDeleteProfileIds)
        assertEquals("settingsavatar resident", controller.state.newUsernameDraft)
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
        val groupId = GroupId("group-one")
        val base = GroupTargetUiState(
            rows = listOf(
                GroupTargetRowUiState(
                    groupId = groupId,
                    displayName = "Owks",
                    canSendNotices = true,
                ),
            ),
        )

        val all = GroupTargetController(runtime, base).selectAllGroupsMode()
        val manual = all.selectManualGroupsMode().setManualGroupSelected(groupId, true)

        assertEquals(GroupTargetMode.ALL, all.state.mode)
        assertFalse(all.state.pickerVisible)
        assertEquals(GroupTargetMode.MANUAL, manual.state.mode)
        assertTrue(manual.state.pickerVisible)
        assertEquals(1, manual.state.selectedCount)
        assertTrue(manual.state.rows.single().selected)
    }

    @Test
    fun inventoryControllerOwnsShortcutFolderAndAssetProjection() {
        val itemId = InventoryItemId("welcome-area")
        val folderId = InventoryFolderId("landmarks")
        val base = InventoryBrowserUiState(
            visibleAssetRows = listOf(
                InventoryAssetRowUiState(
                    itemId = itemId,
                    displayName = "Welcome Area",
                    kind = InventoryItemKind.LANDMARK,
                ),
            ),
        )

        val controller = InventoryBrowserController(runtime, base)
            .openInventoryShortcut(InventoryShortcut.TEXTURES)
            .openInventoryFolder(folderId)
            .selectInventoryAsset(itemId)

        assertTrue(controller.state.shortcuts.texturesSelected)
        assertEquals(folderId, controller.state.currentFolderId)
        assertTrue(controller.state.visibleAssetRows.single().selected)
    }
}
