package org.gem.apps.android

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.gem.ui.GemApp
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class GemAndroidNativeUiInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<GemAndroidActivity>()

    @Test
    fun launcherRendersLoginChromeAndLoggedOutMenu() {
        waitForNode(GemTestTags.ViewLogin)
        composeRule.onNodeWithTag(GemTestTags.MenuButton).assertIsDisplayed()
        composeRule.onNodeWithTag(GemTestTags.SecondLifeTime).assertIsDisplayed()

        openMenu()

        composeRule.onNodeWithTag(GemTestTags.AppMenu).assertIsDisplayed()
        composeRule.onNodeWithTag(GemTestTags.OpenSettings).assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag(GemTestTags.LogOut).assertIsDisplayed().assertIsNotEnabled()
        composeRule.onNodeWithTag(GemTestTags.Exit).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun settingsBackReturnsToLoginWithoutFinishingActivity() {
        waitForNode(GemTestTags.ViewLogin)
        openMenu()
        composeRule.onNodeWithTag(GemTestTags.OpenSettings).performClick()
        waitForNode(GemTestTags.ViewSettings)

        pressAndroidBack()

        waitForNode(GemTestTags.ViewLogin)
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun loginRootBackFinishesActivity() {
        waitForNode(GemTestTags.ViewLogin)

        pressAndroidBack()

        waitForActivityFinish()
    }

    @Test
    fun fakeRuntimeLoginReachesComposeAndScrollableSurfaces() {
        installFakeRuntime()
        composeRule.onNodeWithTag(GemTestTags.AccountPassword)
            .performScrollTo()
            .assertIsDisplayed()
        passwordEditableField()
            .performTextInput("!")
        composeRule.onNodeWithTag(GemTestTags.AccountPassword).assertIsDisplayed()

        loginWithFakeRuntime()

        composeRule.onNodeWithTag(GemTestTags.InventoryList).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag(GemTestTags.ManualGroups).performScrollTo().performClick()
        composeRule.onNodeWithTag(GemTestTags.GroupList).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fakeRuntimeLogoutReturnsToLoginWithoutFinishingActivity() {
        loginWithFakeRuntime()

        openMenu()
        composeRule.onNodeWithTag(GemTestTags.LogOut).assertIsEnabled().performClick()

        waitForNode(GemTestTags.ViewLogin)
        assertFalse(composeRule.activity.isFinishing)
    }

    @Test
    fun fakeRuntimeExitLogsOutAndFinishesActivity() {
        loginWithFakeRuntime()

        openMenu()
        composeRule.onNodeWithTag(GemTestTags.Exit).performClick()

        waitForActivityFinish()
    }

    @Test
    fun fakeRuntimeComposeRootBackLogsOutAndFinishesActivity() {
        loginWithFakeRuntime()

        pressAndroidBack()

        waitForActivityFinish()
    }

    @Test
    fun settingsThemeToggleLabelsRemainReachable() {
        waitForNode(GemTestTags.ViewLogin)
        openMenu()
        composeRule.onNodeWithTag(GemTestTags.OpenSettings).performClick()
        waitForNode(GemTestTags.ViewSettings)

        composeRule.onNodeWithText(text(GemTextKey.Light)).assertIsDisplayed()
        composeRule.onNodeWithText(text(GemTextKey.Dark)).assertIsDisplayed()
    }

    private fun installFakeRuntime() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.setContent {
                GemApp(
                    runtime = GemAndroidUiTestRuntime.ready(),
                    onExitReady = { composeRule.activity.finish() },
                )
            }
        }
        waitForNode(GemTestTags.ViewLogin)
    }

    private fun loginWithFakeRuntime() {
        installFakeRuntime()
        composeRule.onNodeWithTag(GemTestTags.LoginButton).performScrollTo().performClick()
        waitForNode(GemTestTags.ViewCompose)
    }

    private fun openMenu() {
        composeRule.onNodeWithTag(GemTestTags.MenuButton).performClick()
        waitForNode(GemTestTags.AppMenu)
    }

    private fun passwordEditableField() =
        composeRule.onNode(
            hasSetTextAction() and hasAnyAncestor(hasTestTag(GemTestTags.AccountPassword)),
        )

    private fun pressAndroidBack() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun waitForNode(tag: String) {
        composeRule.waitUntil(timeoutMillis = UiWaitMillis) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitForActivityFinish() {
        composeRule.waitUntil(timeoutMillis = UiWaitMillis) {
            composeRule.activity.isFinishing || composeRule.activity.isDestroyed
        }
    }

    private fun text(key: GemTextKey): String = EnglishGemTextCatalogue.text(key)

    private companion object {
        const val UiWaitMillis: Long = 10_000L
    }
}
