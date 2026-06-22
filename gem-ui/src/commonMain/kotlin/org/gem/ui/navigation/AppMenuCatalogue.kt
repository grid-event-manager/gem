package org.gem.ui.navigation

import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextKey

object AppMenuCatalogue {
    fun entries(activeSession: Boolean): List<AppMenuEntry> =
        listOf(
            AppMenuEntry.SectionEntry(
                section = AppSectionCatalogue.accounts,
                testTag = GemTestTags.OpenAccounts,
            ),
            AppMenuEntry.SectionEntry(
                section = AppSectionCatalogue.settings,
                testTag = GemTestTags.OpenSettings,
            ),
            AppMenuEntry.SectionEntry(
                section = AppSectionCatalogue.about,
                testTag = GemTestTags.OpenAbout,
            ),
            AppMenuEntry.CommandEntry(
                command = AppMenuCommand.LogOut,
                labelKey = GemTextKey.LogOut,
                enabled = activeSession,
                testTag = GemTestTags.LogOut,
                dividerBefore = false,
            ),
            AppMenuEntry.CommandEntry(
                command = AppMenuCommand.Exit,
                labelKey = GemTextKey.Exit,
                enabled = true,
                testTag = GemTestTags.Exit,
                dividerBefore = true,
            ),
        )
}
