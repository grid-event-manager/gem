package org.hostess.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.hostess.ui.components.HostessAppScaffold
import org.hostess.ui.components.HostessSendFooter
import org.hostess.ui.components.HostessTopBar
import org.hostess.ui.components.SessionStrip
import org.hostess.ui.components.SettingsBackNav
import org.hostess.ui.controllers.GroupTargetController
import org.hostess.ui.controllers.HostessAppController
import org.hostess.ui.controllers.InventoryBrowserController
import org.hostess.ui.controllers.LoginController
import org.hostess.ui.controllers.NoticeComposerController
import org.hostess.ui.controllers.SettingsController
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.screens.ComposeScreen
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.screens.LoginScreen
import org.hostess.ui.screens.SettingsScreen
import org.hostess.ui.state.UiRoute
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun HostessApp(
    runtime: HostessUiRuntime,
    textCatalogue: HostessTextCatalogue = EnglishHostessTextCatalogue,
) {
    var appController by remember(runtime) { mutableStateOf(HostessAppController(runtime)) }
    var loginController by remember(runtime) { mutableStateOf(LoginController(runtime).refreshSavedLogins()) }
    var settingsController by remember(runtime) { mutableStateOf(SettingsController(runtime).refreshSavedAccounts()) }
    var noticeController by remember(runtime) { mutableStateOf(NoticeComposerController(runtime)) }
    var groupTargetController by remember(runtime) { mutableStateOf(GroupTargetController(runtime)) }
    var inventoryController by remember(runtime) { mutableStateOf(InventoryBrowserController(runtime)) }

    fun routeAfterLogin(controller: LoginController) {
        appController = HostessAppController(runtime, controller.appState)
        if (controller.appState.route == UiRoute.Compose) {
            val composeNoticeController = NoticeComposerController(
                runtime = runtime,
                session = controller.appState.session,
                avatarReady = true,
            )
            groupTargetController = composeNoticeController.refreshGroups()
            noticeController = composeNoticeController.updateTargetSet(groupTargetController.targetSet)
            inventoryController = InventoryBrowserController(
                runtime = runtime,
                session = controller.appState.session,
            ).refreshInventory()
        }
    }

    HostessTheme.Provide {
        val route = appController.state.route
        HostessAppScaffold(
            topBar = {
                HostessTopBar(
                    activeAccountLabel = appController.state.activeAccountLabel,
                    menuOpen = appController.state.menuOpen,
                    textCatalogue = textCatalogue,
                    onMenuClick = { appController = appController.openMenu() },
                    onMenuDismiss = { appController = appController.closeMenu() },
                    onSettingsClick = {
                        val opened = appController.openSettings()
                        appController = opened
                        settingsController = SettingsController(runtime, appState = opened.state).refreshSavedAccounts()
                    },
                    onLogoutClick = {
                        appController = appController.logout()
                        loginController = LoginController(runtime).refreshSavedLogins()
                        settingsController = SettingsController(
                            runtime,
                            appState = appController.state,
                        ).refreshSavedAccounts()
                        noticeController = NoticeComposerController(runtime)
                        groupTargetController = GroupTargetController(runtime)
                        inventoryController = InventoryBrowserController(runtime)
                    },
                )
            },
            navigation = if (route == UiRoute.Settings) {
                {
                    SettingsBackNav(
                        text = textCatalogue.text(HostessTextKey.Back),
                        onBack = {
                            appController = appController.backFromSettings()
                        },
                    )
                }
            } else {
                null
            },
            sessionStrip = {
                SessionStrip(
                    state = appController.state.sessionStrip,
                    textCatalogue = textCatalogue,
                )
            },
            footer = if (route == UiRoute.Compose) {
                {
                    HostessSendFooter(
                        state = noticeController.state.sendFooterState,
                        textCatalogue = textCatalogue,
                        onPrimaryAction = {
                            noticeController = noticeController.sendNotices()
                        },
                    )
                }
            } else {
                null
            },
            content = {
                when (route) {
                    UiRoute.Login -> {
                        LoginScreen(
                            state = loginController.state,
                            textCatalogue = textCatalogue,
                            onSavedLoginSelected = { profileId ->
                                loginController = loginController.selectSavedLogin(profileId)
                            },
                            onSavedPasswordVisibilityToggle = {
                                loginController = loginController.toggleSavedPasswordVisibility()
                            },
                            onSavedPasswordChanged = { password ->
                                loginController = loginController.updateSavedPasswordDraft(password)
                            },
                            onAddLoginToggle = {
                                loginController = loginController.toggleAddLoginPanel()
                            },
                            onNewUsernameChanged = { username ->
                                loginController = loginController.updateNewUsernameDraft(username)
                            },
                            onNewPasswordFocus = {
                                loginController = loginController.normalizeNewLoginNameOnPasswordFocus()
                            },
                            onNewPasswordChanged = { password ->
                                loginController = loginController.updateNewPasswordDraft(password)
                            },
                            onNewPasswordVisibilityToggle = {
                                loginController = loginController.toggleNewPasswordVisibility()
                            },
                            onLogin = {
                                loginController = loginController.loginSelected()
                                routeAfterLogin(loginController)
                            },
                            onSaveAndLogin = {
                                loginController = loginController.saveAndLogin()
                                routeAfterLogin(loginController)
                            },
                        )
                    }
                    UiRoute.Compose -> {
                        ComposeScreen(
                            noticeState = noticeController.state,
                            inventoryState = inventoryController.state,
                            groupTargetState = groupTargetController.state,
                            textCatalogue = textCatalogue,
                            onSubjectChanged = { subject ->
                                noticeController = noticeController.updateSubject(subject)
                            },
                            onBodyChanged = { body ->
                                noticeController = noticeController.updateBody(body)
                            },
                            onInventoryShortcutSelected = { shortcut ->
                                inventoryController = inventoryController.openInventoryShortcut(shortcut)
                            },
                            onInventoryFolderOpen = { folderId ->
                                inventoryController = inventoryController.openInventoryFolder(folderId)
                            },
                            onInventoryAssetSelected = { itemId ->
                                inventoryController = inventoryController.selectInventoryAsset(itemId)
                                noticeController = noticeController.updateSelectedAttachment(
                                    inventoryController.state.selectedAttachment,
                                )
                            },
                            onAllGroupsChanged = { selected ->
                                groupTargetController = if (selected) {
                                    groupTargetController.selectAllGroupsMode()
                                } else {
                                    groupTargetController.clearGroupSelectionMode()
                                }
                                noticeController = noticeController.updateTargetSet(groupTargetController.targetSet)
                            },
                            onManualGroupsChanged = { selected ->
                                groupTargetController = if (selected) {
                                    groupTargetController.selectManualGroupsMode()
                                } else {
                                    groupTargetController.clearManualGroupsMode()
                                }
                                noticeController = noticeController.updateTargetSet(groupTargetController.targetSet)
                            },
                            onManualGroupSelected = { displayName, selected ->
                                groupTargetController = groupTargetController.setManualGroupSelected(displayName, selected)
                                noticeController = noticeController.updateTargetSet(groupTargetController.targetSet)
                            },
                        )
                    }
                    UiRoute.Settings -> SettingsScreen(
                        state = settingsController.state,
                        textCatalogue = textCatalogue,
                        onSavedAccountSelected = { profileId ->
                            settingsController = settingsController.selectSavedAccount(profileId)
                        },
                        onSavedPasswordVisibilityToggle = {
                            settingsController = settingsController.toggleSavedPasswordVisibility()
                        },
                        onSavedPasswordChanged = { password ->
                            settingsController = settingsController.updateSavedPasswordDraft(password)
                        },
                        onAddAccountToggle = {
                            settingsController = settingsController.toggleAddAccountPanel()
                        },
                        onNewUsernameChanged = { username ->
                            settingsController = settingsController.updateNewUsernameDraft(username)
                        },
                        onNewPasswordFocus = {
                            settingsController = settingsController.normalizeNewAccountNameOnPasswordFocus()
                        },
                        onNewPasswordChanged = { password ->
                            settingsController = settingsController.updateNewPasswordDraft(password)
                        },
                        onNewPasswordVisibilityToggle = {
                            settingsController = settingsController.toggleNewPasswordVisibility()
                        },
                        onSaveNewAccount = {
                            settingsController = settingsController.saveNewAccount()
                        },
                        onDeleteAccountToggle = {
                            settingsController = settingsController.toggleDeleteAccountPanel()
                        },
                        onDeleteAccountSelected = { profileId, selected ->
                            settingsController = settingsController.setDeleteAccountSelected(profileId, selected)
                        },
                        onOpenDeleteModal = {
                            settingsController = settingsController.openDeleteAccounts()
                        },
                        onConfirmDelete = {
                            settingsController = settingsController.confirmDeleteAccounts()
                            appController = HostessAppController(runtime, settingsController.appState)
                        },
                        onCancelDelete = {
                            settingsController = settingsController.cancelDeleteAccounts()
                        },
                    )
                }
            },
        )
    }
}
