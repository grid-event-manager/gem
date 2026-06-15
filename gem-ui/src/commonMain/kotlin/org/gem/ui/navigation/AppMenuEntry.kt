package org.gem.ui.navigation

import org.gem.ui.text.GemTextKey

sealed interface AppMenuEntry {
    data class SectionEntry(
        val section: AppSection,
        val testTag: String,
    ) : AppMenuEntry

    data class CommandEntry(
        val command: AppMenuCommand,
        val labelKey: GemTextKey,
        val enabled: Boolean,
        val testTag: String,
        val dividerBefore: Boolean,
    ) : AppMenuEntry
}
