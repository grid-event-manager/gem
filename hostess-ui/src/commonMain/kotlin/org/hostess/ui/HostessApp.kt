package org.hostess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.ui.components.HostessSendFooter
import org.hostess.ui.components.HostessTopBar
import org.hostess.ui.components.SessionStrip
import org.hostess.ui.controllers.HostessAppController
import org.hostess.ui.controllers.LoginController
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.screens.ComposeScreen
import org.hostess.ui.runtime.HostessUiRuntime
import org.hostess.ui.screens.LoginScreen
import org.hostess.ui.screens.SettingsScreen
import org.hostess.ui.state.UiRoute
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextCatalogue

@Composable
fun HostessApp(
    runtime: HostessUiRuntime,
    textCatalogue: HostessTextCatalogue = EnglishHostessTextCatalogue,
) {
    var appController by remember(runtime) { mutableStateOf(HostessAppController(runtime)) }
    var loginController by remember(runtime) { mutableStateOf(LoginController(runtime).refreshSavedLogins()) }

    HostessTheme.Provide {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(HostessTheme.colors.page)
                .padding(HostessTheme.spacing.pagePadding)
                .testTag(HostessTestTags.HostessApp),
        ) {
            HostessTopBar(
                activeAccountLabel = appController.state.activeAccountLabel,
                menuOpen = appController.state.menuOpen,
                textCatalogue = textCatalogue,
                onMenuClick = { appController = appController.openMenu() },
                onMenuDismiss = { appController = appController.closeMenu() },
                onSettingsClick = { appController = appController.openSettings() },
                onLogoutClick = {
                    appController = appController.logout()
                    loginController = LoginController(runtime).refreshSavedLogins()
                },
            )
            SessionStrip(
                state = appController.state.sessionStrip,
                textCatalogue = textCatalogue,
            )
            when (appController.state.route) {
                UiRoute.Login -> LoginScreen(
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
                        appController = HostessAppController(runtime, loginController.appState)
                    },
                    onSaveAndLogin = {
                        loginController = loginController.saveAndLogin()
                        appController = HostessAppController(runtime, loginController.appState)
                    },
                )
                UiRoute.Compose -> {
                    ComposeScreen()
                    HostessSendFooter(
                        state = appController.state.sendFooter,
                        textCatalogue = textCatalogue,
                        onPrimaryAction = {},
                    )
                }
                UiRoute.Settings -> SettingsScreen()
            }
        }
    }
}
