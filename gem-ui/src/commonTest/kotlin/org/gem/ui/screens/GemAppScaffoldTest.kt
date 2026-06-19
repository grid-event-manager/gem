package org.gem.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileId
import org.gem.ui.appearanceModeForNavigationToggleChecked
import org.gem.ui.applyNavigationThemeToggle
import org.gem.ui.components.GemAppScaffold
import org.gem.ui.components.SectionBackNav
import org.gem.ui.controllers.AppearanceController
import org.gem.ui.design.GemSpacing
import org.gem.ui.sectionNavigationShowsThemeToggle
import org.gem.ui.testing.FakeGemUiRuntime
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextKey
import org.gem.ui.topBarTitleForRoute
import org.gem.ui.state.UiRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemAppScaffoldTest {
    @Test
    fun `scaffold contract uses shared phone form tokens and app tag`() {
        val spacing = GemSpacing()

        assertEquals("data-gem-app", GemTestTags.GemApp)
        assertEquals(412.dp, spacing.shellMaxWidth)
        assertEquals(14.dp, spacing.pagePadding)
        assertEquals(10.dp, spacing.rowGap)
    }

    @Test
    fun `scaffold contract accepts stable named slots`() {
        val scaffold: @Composable () -> Unit = {
            GemAppScaffold(
                topBar = {},
                navigation = {},
                sessionStrip = {},
                content = {},
                footer = {},
            )
        }

        assertNotNull(scaffold)
    }

    @Test
    fun `top bar title model preserves brand on root routes`() {
        val login = topBarTitleForRoute(UiRoute.Login)
        val compose = topBarTitleForRoute(UiRoute.Compose)

        assertEquals(GemTextKey.BrandInitials, login.titleKey)
        assertEquals(GemTextKey.BrandSubtitle, login.subtitleKey)
        assertEquals(GemTextKey.BrandInitials, compose.titleKey)
        assertEquals(GemTextKey.BrandSubtitle, compose.subtitleKey)
    }

    @Test
    fun `top bar title model uses section labels without subtitle on section routes`() {
        val settings = topBarTitleForRoute(UiRoute.Settings)
        val accounts = topBarTitleForRoute(UiRoute.Accounts)

        assertEquals(GemTextKey.Settings, settings.titleKey)
        assertNull(settings.subtitleKey)
        assertEquals(GemTextKey.Accounts, accounts.titleKey)
        assertNull(accounts.subtitleKey)
    }

    @Test
    fun `section navigation toggle is settings only`() {
        assertTrue(sectionNavigationShowsThemeToggle(UiRoute.Settings))
        assertFalse(sectionNavigationShowsThemeToggle(UiRoute.Accounts))
        assertFalse(sectionNavigationShowsThemeToggle(UiRoute.Login))
        assertFalse(sectionNavigationShowsThemeToggle(UiRoute.Compose))
    }

    @Test
    fun `navigation theme toggle maps checked state to manual theme mode`() {
        assertEquals(AppearanceMode.DARK, appearanceModeForNavigationToggleChecked(true))
        assertEquals(AppearanceMode.LIGHT, appearanceModeForNavigationToggleChecked(false))
    }

    @Test
    fun `navigation theme toggle uses manual theme route and clears selected profile`() {
        val selected = AppearanceController.initial(FakeGemUiRuntime.ready(), osDark = false)
            .selectProfile(AppearanceProfileId("stock-goth-dark"))

        val toggled = applyNavigationThemeToggle(
            controller = selected,
            checked = false,
            osDark = true,
        )

        assertEquals(AppearanceMode.LIGHT, toggled.state.mode)
        assertNull(toggled.state.selectedProfileId)
        assertFalse(toggled.state.toggleChecked)
    }

    @Test
    fun `section back navigation accepts a single optional trailing slot`() {
        val navigation: @Composable () -> Unit = {
            SectionBackNav(
                text = "BACK",
                onBack = {},
                trailingContent = {},
            )
        }

        assertNotNull(navigation)
    }
}
