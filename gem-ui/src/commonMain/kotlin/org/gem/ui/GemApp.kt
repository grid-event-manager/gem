package org.gem.ui

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
import org.gem.core.appearance.AppearanceMode
import org.gem.ui.components.AppearancePanelCallbacks
import org.gem.ui.components.GemAppScaffold
import org.gem.ui.components.GemOperationModal
import org.gem.ui.components.GemPlatformBackHandler
import org.gem.ui.components.GemSendFooter
import org.gem.ui.components.GemTopBar
import org.gem.ui.components.GemTopBarSubtitle
import org.gem.ui.components.GemTopBarTitle
import org.gem.ui.components.SessionStrip
import org.gem.ui.components.SectionBackNav
import org.gem.ui.components.ThemeModeToggle
import org.gem.ui.controllers.AccountsController
import org.gem.ui.controllers.AppearanceController
import org.gem.ui.controllers.GroupTargetController
import org.gem.ui.controllers.GemAppController
import org.gem.ui.controllers.GemLogoutWorkflow
import org.gem.ui.controllers.GemLogoutWorkflowAction
import org.gem.ui.controllers.InventoryBrowserController
import org.gem.ui.controllers.LoginController
import org.gem.ui.controllers.NoticeComposerController
import org.gem.ui.design.AppearanceDesignTokenMapper
import org.gem.ui.design.GemTheme
import org.gem.ui.navigation.AppMenuCatalogue
import org.gem.ui.navigation.AppMenuCommand
import org.gem.ui.navigation.AppSectionCatalogue
import org.gem.ui.navigation.SectionBackPolicy
import org.gem.ui.navigation.SectionSessionStripPolicy
import org.gem.ui.screens.ComposeScreen
import org.gem.ui.runtime.GemUiRuntime
import org.gem.ui.screens.LoginScreen
import org.gem.ui.screens.AccountsScreen
import org.gem.ui.screens.SettingsScreen
import org.gem.ui.state.AppearanceUiState
import org.gem.ui.state.LoginEntryMode
import org.gem.ui.state.UiRoute
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.AppearanceProfileDisplayLabel
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey
import org.gem.ui.time.SecondLifeTimeService

@Composable
fun GemApp(
    runtime: GemUiRuntime,
    textCatalogue: GemTextCatalogue = EnglishGemTextCatalogue,
    exitRequestSerial: Int = 0,
    onExitReady: () -> Unit = {},
) {
    var appController by remember(runtime) { mutableStateOf(GemAppController(runtime)) }
    var loginController by remember(runtime) { mutableStateOf(LoginController(runtime).refreshSavedLogins()) }
    var accountsController by remember(runtime) { mutableStateOf(AccountsController(runtime).refreshSavedAccounts()) }
    var noticeController by remember(runtime) { mutableStateOf(NoticeComposerController(runtime)) }
    var groupTargetController by remember(runtime) { mutableStateOf(GroupTargetController(runtime)) }
    var inventoryController by remember(runtime) { mutableStateOf(InventoryBrowserController(runtime)) }
    var sendFeedbackGeneration by remember(runtime) { mutableStateOf(0) }
    var logoutInFlight by remember(runtime) { mutableStateOf(false) }
    var exitAfterCurrentLogout by remember(runtime) { mutableStateOf(false) }
    val secondLifeTimeService = remember(runtime) { SecondLifeTimeService(runtime.clockPort) }
    var secondLifeTimeDisplay by remember(runtime) {
        mutableStateOf(secondLifeTimeService.currentSnapshot().display)
    }
    val coroutineScope = rememberCoroutineScope()
    val osDark = isSystemInDarkTheme()
    var appearanceController by remember(runtime) { mutableStateOf(AppearanceController.initial(runtime, osDark)) }
    LaunchedEffect(runtime, osDark) {
        appearanceController = appearanceController.refresh(osDark)
    }
    LaunchedEffect(runtime) {
        while (true) {
            val snapshot = secondLifeTimeService.currentSnapshot()
            secondLifeTimeDisplay = snapshot.display
            delay(millisUntilNextMinute(snapshot.epochMilliseconds))
        }
    }

    fun refreshLoginFromCredentialStore() {
        loginController = loginController.refreshSavedLogins()
    }

    fun refreshAccountsFromCredentialStore() {
        accountsController = AccountsController(runtime, appState = appController.state).refreshSavedAccounts()
    }

    fun runSectionBackNavigation(policy: SectionBackPolicy) {
        appController = appController.backFromSection(policy)
        refreshLoginFromCredentialStore()
        refreshAccountsFromCredentialStore()
    }

    fun openMenuSection(route: UiRoute) {
        val opened = appController.openSection(route)
        appController = opened
        if (route == UiRoute.Accounts) {
            refreshAccountsFromCredentialStore()
        }
    }

    fun resetControllersAfterLogout(loggedOut: GemAppController) {
        appController = loggedOut
        loginController = loginController.refreshSavedLogins()
        accountsController = AccountsController(
            runtime,
            appState = loggedOut.state,
        ).refreshSavedAccounts()
        noticeController = NoticeComposerController(runtime)
        groupTargetController = GroupTargetController(runtime)
        inventoryController = InventoryBrowserController(runtime)
    }

    fun runLogoutWorkflow(exitAfterLogout: Boolean = false) {
        val decision = GemLogoutWorkflow.decide(
            hasActiveSession = appController.state.session != null,
            logoutInFlight = logoutInFlight,
            exitAfterLogout = exitAfterLogout,
            existingExitAfterCurrentLogout = exitAfterCurrentLogout,
        )
        when (decision.action) {
            GemLogoutWorkflowAction.EXIT_IMMEDIATELY -> {
                if (decision.exitAfterLogout) {
                    onExitReady()
                }
                return
            }
            GemLogoutWorkflowAction.MERGE_WITH_IN_FLIGHT_LOGOUT -> {
                exitAfterCurrentLogout = decision.exitAfterLogout
                return
            }
            GemLogoutWorkflowAction.START_LOGOUT -> Unit
        }

        exitAfterCurrentLogout = decision.exitAfterLogout
        logoutInFlight = true
        val loggingOut = appController.beginLogout()
        appController = loggingOut
        coroutineScope.launch {
            delay(LogoutSpinnerMinimumMillis)
            val loggedOut = withContext(Dispatchers.Default) {
                GemAppController(runtime, loggingOut.state).logout()
            }
            resetControllersAfterLogout(loggedOut)
            logoutInFlight = false
            if (exitAfterCurrentLogout) {
                exitAfterCurrentLogout = false
                onExitReady()
            }
        }
    }

    fun runMenuCommand(command: AppMenuCommand) {
        when (command) {
            AppMenuCommand.LogOut -> runLogoutWorkflow()
            AppMenuCommand.Exit -> runLogoutWorkflow(exitAfterLogout = true)
        }
    }

    fun runBackWorkflow() {
        when {
            appController.state.menuOpen -> {
                appController = appController.closeMenu()
            }
            AppSectionCatalogue.sectionFor(appController.state.route).backPolicy ==
                SectionBackPolicy.ReturnToSessionOrLogin -> {
                runSectionBackNavigation(
                    AppSectionCatalogue.sectionFor(appController.state.route).backPolicy,
                )
            }
            appController.state.route == UiRoute.Compose && appController.state.session != null -> {
                runLogoutWorkflow(exitAfterLogout = true)
            }
            else -> {
                onExitReady()
            }
        }
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
                current = current.showOperation(GemTextKey.SendingLoginDetails)
                loginController = current
            }
            current = withContext(Dispatchers.Default) {
                current.completeAuthentication()
            }
            loginController = current

            if (current.state.operation.inFlight && current.appState.session != null) {
                current = current.showOperation(GemTextKey.RezzingWorld)
                loginController = current
                var avatarProgressJob: Job? = coroutineScope.launch {
                    delay(AvatarProgressDetailMillis)
                    if (loginController.state.operation.messageKey == GemTextKey.RezzingWorld) {
                        loginController = loginController.showOperation(GemTextKey.LoadingAvatar)
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
                appController = GemAppController(runtime, current.appState)
                current = current.showOperation(GemTextKey.LoadingGroups)
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
                current = current.showOperation(GemTextKey.LoadingInventory)
                loginController = current
                inventoryController = withContext(Dispatchers.Default) {
                    InventoryBrowserController(
                        runtime = runtime,
                        session = current.appState.session,
                    ).refreshInventory()
                }
                loginController = current.finishLoginOperation()
            } else {
                appController = GemAppController(runtime, current.appState)
            }
        }
    }

    LaunchedEffect(exitRequestSerial) {
        if (exitRequestSerial > 0) {
            runLogoutWorkflow(exitAfterLogout = true)
        }
    }

    GemTheme.Provide(
        tokens = AppearanceDesignTokenMapper.tokens(
            draft = appearanceController.state.currentDraft,
            availableFonts = appearanceController.state.availableFontFamilies,
            platformFontFamilyResolver = runtime.platformFontFamilyResolver,
        ),
    ) {
        val route = appController.state.route
        val activeSection = AppSectionCatalogue.sectionFor(route)
        val menuEntries = AppMenuCatalogue.entries(activeSession = appController.state.session != null)
        GemPlatformBackHandler(
            enabled = true,
            onBack = { runBackWorkflow() },
        )
        GemAppScaffold(
            topBar = {
                GemTopBar(
                    title = topBarTitleForRoute(
                        route = route,
                        appearanceState = appearanceController.state,
                        textCatalogue = textCatalogue,
                    ),
                    activeAccountLabel = appController.state.activeAccountLabel,
                    secondLifeTimeDisplay = secondLifeTimeDisplay,
                    menuOpen = appController.state.menuOpen,
                    menuEntries = menuEntries,
                    textCatalogue = textCatalogue,
                    onMenuClick = { appController = appController.openMenu() },
                    onMenuDismiss = { appController = appController.closeMenu() },
                    onMenuSectionSelected = ::openMenuSection,
                    onMenuCommandSelected = ::runMenuCommand,
                )
            },
            navigation = if (activeSection.backPolicy == SectionBackPolicy.ReturnToSessionOrLogin) {
                {
                    SectionBackNav(
                        text = textCatalogue.text(GemTextKey.Back),
                        onBack = {
                            runSectionBackNavigation(activeSection.backPolicy)
                        },
                        testTag = if (route == UiRoute.Accounts) GemTestTags.AccountsBack else null,
                        trailingContent = if (sectionNavigationShowsThemeToggle(route)) {
                            {
                                ThemeModeToggle(
                                    checked = appearanceController.state.toggleChecked,
                                    lightLabel = textCatalogue.text(GemTextKey.Light),
                                    darkLabel = textCatalogue.text(GemTextKey.Dark),
                                    onCheckedChange = { checked ->
                                        appearanceController = applyNavigationThemeToggle(
                                            controller = appearanceController,
                                            checked = checked,
                                            osDark = osDark,
                                        )
                                    },
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            } else {
                null
            },
            sessionStrip = {
                if (activeSection.sessionStripPolicy == SectionSessionStripPolicy.UseAppState) {
                    SessionStrip(
                        state = appController.state.sessionStrip,
                        textCatalogue = textCatalogue,
                    )
                }
            },
            footer = if (route == UiRoute.Compose) {
                {
                    GemSendFooter(
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
                    UiRoute.Accounts -> AccountsScreen(
                        state = accountsController.state,
                        textCatalogue = textCatalogue,
                        onEditAccountToggle = {
                            accountsController = accountsController.toggleEditAccountPanel()
                        },
                        onSavedAccountSelected = { profileId ->
                            accountsController = accountsController.selectSavedAccount(profileId)
                        },
                        onSavedPasswordVisibilityToggle = {
                            accountsController = accountsController.toggleSavedPasswordVisibility()
                        },
                        onSavedPasswordChanged = { password ->
                            accountsController = accountsController.updateSavedPasswordDraft(password)
                        },
                        onSaveEditedPassword = {
                            accountsController = accountsController.saveEditedPassword()
                            refreshLoginFromCredentialStore()
                        },
                        onAddAccountToggle = {
                            accountsController = accountsController.toggleAddAccountPanel()
                        },
                        onNewUsernameChanged = { username ->
                            accountsController = accountsController.updateNewUsernameDraft(username)
                        },
                        onNewPasswordFocus = {
                            accountsController = accountsController.normalizeNewAccountNameOnPasswordFocus()
                        },
                        onNewPasswordChanged = { password ->
                            accountsController = accountsController.updateNewPasswordDraft(password)
                        },
                        onNewPasswordVisibilityToggle = {
                            accountsController = accountsController.toggleNewPasswordVisibility()
                        },
                        onSaveNewAccount = {
                            accountsController = accountsController.saveNewAccount()
                            refreshLoginFromCredentialStore()
                        },
                        onDeleteAccountSelected = { profileId, selected ->
                            accountsController = accountsController.setDeleteAccountSelected(profileId, selected)
                        },
                        onOpenDeleteModal = {
                            accountsController = accountsController.openDeleteAccounts()
                        },
                        onConfirmDelete = {
                            accountsController = accountsController.confirmDeleteAccounts()
                            appController = GemAppController(runtime, accountsController.appState)
                            refreshLoginFromCredentialStore()
                            refreshAccountsFromCredentialStore()
                        },
                        onCancelDelete = {
                            accountsController = accountsController.cancelDeleteAccounts()
                        },
                    )
                    UiRoute.Settings -> SettingsScreen(
                        appearanceState = appearanceController.state,
                        textCatalogue = textCatalogue,
                        callbacks = AppearancePanelCallbacks(
                            onExpandedPanelChanged = { panel ->
                                appearanceController = appearanceController.setExpandedPanel(panel)
                            },
                            onTextTargetSelectorOpened = {
                                appearanceController = appearanceController.openTextTargetSelector()
                            },
                            onElementTargetSelectorOpened = {
                                appearanceController = appearanceController.openElementTargetSelector()
                            },
                            onTextTargetSelected = { target ->
                                appearanceController = appearanceController.selectTextTarget(target)
                            },
                            onElementTargetSelected = { target ->
                                appearanceController = appearanceController.selectElementTarget(target)
                            },
                            onFontSelected = { family ->
                                appearanceController = appearanceController.updateFont(family)
                            },
                            onColorSelected = { color ->
                                appearanceController = appearanceController.updateColor(color)
                            },
                            onRgbValueChanged = { channel, value ->
                                appearanceController = appearanceController.updateRgb(channel, value)
                            },
                            onRgbInputInvalid = { channel ->
                                appearanceController = appearanceController.updateRgb(channel, -1)
                            },
                            onHexChanged = { value ->
                                appearanceController = appearanceController.updateHex(value)
                            },
                            onOpenSaveThemeDialog = {
                                appearanceController = appearanceController.openSaveThemeDialog()
                            },
                            onCloseSaveThemeDialog = {
                                appearanceController = appearanceController.closeSaveThemeDialog()
                            },
                            onSaveThemeNameChanged = { name ->
                                appearanceController = appearanceController.updateSaveThemeName(name)
                            },
                            onSaveThemeModeChanged = { mode ->
                                appearanceController = appearanceController.updateSaveThemeMode(mode)
                            },
                            onSaveTheme = { name, mode ->
                                appearanceController = appearanceController.saveTheme(name, mode)
                            },
                            onResetCurrentMode = {
                                appearanceController = appearanceController.resetCurrentMode()
                            },
                            onProfileSelected = { profileId ->
                                appearanceController = appearanceController.selectProfile(profileId)
                            },
                        ),
                    )
                }
            },
        )
        appController.state.blockingOperationMessageKey?.let { messageKey ->
            GemOperationModal(
                visible = true,
                message = textCatalogue.text(messageKey),
            )
        }
    }
}

private const val LogoutSpinnerMinimumMillis: Long = 650L
private const val SendValidationFeedbackMillis: Long = 3_000L
private const val AvatarProgressDetailMillis: Long = 5_000L
private const val ClockMinuteMillis: Long = 60_000L

internal fun topBarTitleForRoute(
    route: UiRoute,
    appearanceState: AppearanceUiState,
    textCatalogue: GemTextCatalogue,
): GemTopBarTitle =
    when (route) {
        UiRoute.Settings -> GemTopBarTitle(
            titleKey = AppSectionCatalogue.sectionFor(route).labelKey,
            subtitle = GemTopBarSubtitle.Data(
                AppearanceProfileDisplayLabel.current(appearanceState, textCatalogue),
            ),
        )
        UiRoute.Accounts -> GemTopBarTitle(
            titleKey = AppSectionCatalogue.sectionFor(route).labelKey,
            subtitle = GemTopBarSubtitle.None,
        )
        UiRoute.Login,
        UiRoute.Compose -> GemTopBarTitle.brand()
    }

internal fun sectionNavigationShowsThemeToggle(route: UiRoute): Boolean =
    route == UiRoute.Settings

internal fun appearanceModeForNavigationToggleChecked(checked: Boolean): AppearanceMode =
    if (checked) AppearanceMode.DARK else AppearanceMode.LIGHT

internal fun applyNavigationThemeToggle(
    controller: AppearanceController,
    checked: Boolean,
    osDark: Boolean,
): AppearanceController =
    controller.setManualTheme(
        mode = appearanceModeForNavigationToggleChecked(checked),
        osDark = osDark,
    )

private fun millisUntilNextMinute(epochMilliseconds: Long): Long {
    val elapsedInMinute = ((epochMilliseconds % ClockMinuteMillis) + ClockMinuteMillis) % ClockMinuteMillis
    return if (elapsedInMinute == 0L) ClockMinuteMillis else ClockMinuteMillis - elapsedInMinute
}
