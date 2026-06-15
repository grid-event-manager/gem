package org.gem.ui.navigation

import org.gem.ui.state.UiRoute
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppSectionCatalogueTest {
    @Test
    fun sectionCatalogueOwnsRouteOrderPoliciesAndLabels() {
        assertEquals(
            listOf("login", "compose", "accounts", "settings"),
            AppSectionCatalogue.sections.map { it.sectionId },
        )

        assertSame(AppSectionCatalogue.login, AppSectionCatalogue.sectionFor(UiRoute.Login))
        assertSame(AppSectionCatalogue.compose, AppSectionCatalogue.sectionFor(UiRoute.Compose))
        assertSame(AppSectionCatalogue.accounts, AppSectionCatalogue.sectionFor(UiRoute.Accounts))
        assertSame(AppSectionCatalogue.settings, AppSectionCatalogue.sectionFor(UiRoute.Settings))

        assertEquals(GemTextKey.Accounts, AppSectionCatalogue.accounts.labelKey)
        assertEquals(SectionBackPolicy.ReturnToSessionOrLogin, AppSectionCatalogue.accounts.backPolicy)
        assertEquals(SectionSessionStripPolicy.Hidden, AppSectionCatalogue.accounts.sessionStripPolicy)
        assertEquals(GemTextKey.Settings, AppSectionCatalogue.settings.labelKey)
        assertEquals(SectionBackPolicy.ReturnToSessionOrLogin, AppSectionCatalogue.settings.backPolicy)
        assertEquals(SectionSessionStripPolicy.Hidden, AppSectionCatalogue.settings.sessionStripPolicy)
    }

    @Test
    fun menuCatalogueOwnsOrderTagsDividersAndSessionEnablement() {
        val loggedOut = AppMenuCatalogue.entries(activeSession = false)
        val active = AppMenuCatalogue.entries(activeSession = true)

        assertEquals(
            listOf(
                GemTestTags.OpenAccounts,
                GemTestTags.OpenSettings,
                GemTestTags.LogOut,
                GemTestTags.Exit,
            ),
            loggedOut.map {
                when (it) {
                    is AppMenuEntry.SectionEntry -> it.testTag
                    is AppMenuEntry.CommandEntry -> it.testTag
                }
            },
        )
        assertEquals(AppSectionCatalogue.accounts, (loggedOut[0] as AppMenuEntry.SectionEntry).section)
        assertEquals(AppSectionCatalogue.settings, (loggedOut[1] as AppMenuEntry.SectionEntry).section)
        assertEquals(AppMenuCommand.LogOut, (loggedOut[2] as AppMenuEntry.CommandEntry).command)
        assertFalse((loggedOut[2] as AppMenuEntry.CommandEntry).enabled)
        assertFalse((loggedOut[2] as AppMenuEntry.CommandEntry).dividerBefore)
        assertEquals(AppMenuCommand.Exit, (loggedOut[3] as AppMenuEntry.CommandEntry).command)
        assertTrue((loggedOut[3] as AppMenuEntry.CommandEntry).enabled)
        assertTrue((loggedOut[3] as AppMenuEntry.CommandEntry).dividerBefore)
        assertTrue((active[2] as AppMenuEntry.CommandEntry).enabled)
    }
}
