package org.hostess.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.hostess.ui.components.HostessAppScaffold
import org.hostess.ui.components.HostessOperationModal
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
import org.hostess.ui.controllers.ThemeController
import org.hostess.ui.design.HaccuHostessPaletteProvider
import org.hostess.ui.design.HostessDesignTokens
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.design.ResolvedThemeMode
import org.hostess.ui.screens.ComposeScreen
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.screens.LoginScreen
import org.hostess.ui.screens.SettingsScreen
import org.hostess.ui.state.LoginEntryMode
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
    var sendFeedbackGeneration by remember(runtime) { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val osDark = isSystemInDarkTheme()
    var themeController by remember(runtime) { mutableStateOf(ThemeController.initial(runtime, osDark)) }
    LaunchedEffect(runtime, osDark) {
        themeController = themeController.refresh(osDark)
    }

    fun refreshLoginFromCredentialStore() {
        loginController = loginController.refreshSavedLogins()
    }

    fun refreshSettingsFromCredentialStore() {
        settingsController = SettingsController(runtime, appState = appController.state).refreshSavedAccounts()
    }

    fun runLoginWorkflow() {
        val started = loginController
            .normalizeLoginNameOnPasswordFocus()
            .beginLogin()
        loginController = started
        if (!started.state.operation.inFlight) {
            return
        }

        coroutineScope.launch {
            var current = started
            if (current.state.entryMode is LoginEntryMode.New) {
                yield()
                current = current.showOperation(HostessTextKey.SendingLoginDetails)
                loginController = current
            }
            current = withContext(Dispatchers.Default) {
                current.completeAuthentication()
            }
            loginController = current

            if (current.state.operation.inFlight && current.appState.session != null) {
                current = current.showOperation(HostessTextKey.RezzingWorld)
                loginController = current
                var avatarProgressJob: Job? = coroutineScope.launch {
                    delay(AvatarProgressDetailMillis)
                    if (loginController.state.operation.messageKey == HostessTextKey.RezzingWorld) {
                        loginController = loginController.showOperation(HostessTextKey.LoadingAvatar)
                    }
                }
                current = withContext(Dispatchers.Default) {
                    current.completeAvatarReadiness()
                }
                avatarProgressJob?.cancel()
                avatarProgressJob = null
                loginController = current
            }

            if (current.appState.route == UiRoute.Compose) {
                appController = HostessAppController(runtime, current.appState)
                current = current.showOperation(HostessTextKey.LoadingGroups)
                loginController = current
                val composeNoticeController = NoticeComposerController(
                    runtime = runtime,
                    session = current.appState.session,
                    avatarReady = true,
                )
                groupTargetController = withContext(Dispatchers.Default) {
                    composeNoticeController.refreshGroups()
                }
                noticeController = composeNoticeController.updateTargetSet(groupTargetController.targetSet)
                current = current.showOperation(HostessTextKey.LoadingInventory)
                loginController = current
                inventoryController = withContext(Dispatchers.Default) {
                    InventoryBrowserController(
                        runtime = runtime,
                        session = current.appState.session,
                    ).refreshInventory()
                }
                loginController = current.finishLoginOperation()
            } else {
                appController = HostessAppController(runtime, current.appState)
            }
        }
    }

    HostessTheme.Provide(
        tokens = HostessDesignTokens(
            colors = HaccuHostessPaletteProvider.colors(themeController.state.resolvedMode),
        ),
    ) {
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
                        refreshSettingsFromCredentialStore()
                    },
                    onLogoutClick = {
                        val loggingOut = appController.beginLogout()
                        appController = loggingOut
                        coroutineScope.launch {
                            delay(LogoutSpinnerMinimumMillis)
                            val loggedOut = withContext(Dispatchers.Default) {
                                HostessAppController(runtime, loggingOut.state).logout()
                            }
                            appController = loggedOut
                            loginController = loginController.refreshSavedLogins()
                            settingsController = SettingsController(
                                runtime,
                                appState = loggedOut.state,
                            ).refreshSavedAccounts()
                            noticeController = NoticeComposerController(runtime)
                            groupTargetController = GroupTargetController(runtime)
                            inventoryController = InventoryBrowserController(runtime)
                        }
                    },
                )
            },
            navigation = if (route == UiRoute.Settings) {
                {
                    SettingsBackNav(
                        text = textCatalogue.text(HostessTextKey.Back),
                        onBack = {
                            appController = appController.backFromSettings()
                            refreshLoginFromCredentialStore()
                            refreshSettingsFromCredentialStore()
                        },
                        themeChecked = themeController.state.toggleChecked,
                        themeEnabled = true,
                        lightText = textCatalogue.text(HostessTextKey.Light),
                        darkText = textCatalogue.text(HostessTextKey.Dark),
                        onThemeCheckedChange = { checked ->
                            themeController = themeController.setManualTheme(
                                mode = if (checked) ResolvedThemeMode.DARK else ResolvedThemeMode.LIGHT,
                                osDark = osDark,
                            )
                        },
                    )
                }
            } else {
                null
            },
            sessionStrip = {
                if (route != UiRoute.Settings) {
                    SessionStrip(
                        state = appController.state.sessionStrip,
                        textCatalogue = textCatalogue,
                    )
                }
            },
            footer = if (route == UiRoute.Compose) {
                {
                    HostessSendFooter(
                        state = noticeController.state.sendFooterState,
                        textCatalogue = textCatalogue,
                        onPrimaryAction = {
                            val started = noticeController.beginSend()
                            noticeController = started
                            if (started.state.sendFooterState.sending) {
                                coroutineScope.launch {
                                    noticeController = withContext(Dispatchers.Default) {
                                        started.sendNotices()
                                    }
                                }
                            } else if (!started.state.sendFooterState.enabled &&
                                started.state.sendFooterState.showMissingRequirements
                            ) {
                                val generation = ++sendFeedbackGeneration
                                coroutineScope.launch {
                                    delay(SendValidationFeedbackMillis)
                                    if (sendFeedbackGeneration == generation) {
                                        noticeController = noticeController.hideSendRequirements()
                                    }
                                }
                            }
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
                            onUsernameChanged = { username ->
                                loginController = loginController.updateUsernameDraft(username)
                            },
                            onPasswordFocus = {
                                loginController = loginController.normalizeLoginNameOnPasswordFocus()
                            },
                            onPasswordVisibilityToggle = {
                                loginController = loginController.togglePasswordVisibility()
                            },
                            onPasswordChanged = { password ->
                                loginController = loginController.updatePasswordDraft(password)
                            },
                            onLogin = {
                                runLoginWorkflow()
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
                            onAttachmentCleared = {
                                inventoryController = inventoryController.clearSelectedAttachment()
                                noticeController = noticeController.updateSelectedAttachment(null)
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
                        onEditAccountToggle = {
                            settingsController = settingsController.toggleEditAccountPanel()
                        },
                        onSavedAccountSelected = { profileId ->
                            settingsController = settingsController.selectSavedAccount(profileId)
                        },
                        onSavedPasswordVisibilityToggle = {
                            settingsController = settingsController.toggleSavedPasswordVisibility()
                        },
                        onSavedPasswordChanged = { password ->
                            settingsController = settingsController.updateSavedPasswordDraft(password)
                        },
                        onSaveEditedPassword = {
                            settingsController = settingsController.saveEditedPassword()
                            refreshLoginFromCredentialStore()
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
                            refreshLoginFromCredentialStore()
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
                            refreshLoginFromCredentialStore()
                            refreshSettingsFromCredentialStore()
                        },
                        onCancelDelete = {
                            settingsController = settingsController.cancelDeleteAccounts()
                        },
                    )
                }
            },
        )
        appController.state.blockingOperationMessageKey?.let { messageKey ->
            HostessOperationModal(
                visible = true,
                message = textCatalogue.text(messageKey),
            )
        }
    }
}

private const val LogoutSpinnerMinimumMillis: Long = 650L
private const val SendValidationFeedbackMillis: Long = 3_000L
private const val AvatarProgressDetailMillis: Long = 5_000L
