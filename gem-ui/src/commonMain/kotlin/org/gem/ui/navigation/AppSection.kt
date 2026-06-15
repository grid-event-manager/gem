package org.gem.ui.navigation

import org.gem.ui.state.UiRoute
import org.gem.ui.text.GemTextKey

data class AppSection(
    val sectionId: String,
    val route: UiRoute,
    val labelKey: GemTextKey,
    val backPolicy: SectionBackPolicy,
    val sessionStripPolicy: SectionSessionStripPolicy,
)
