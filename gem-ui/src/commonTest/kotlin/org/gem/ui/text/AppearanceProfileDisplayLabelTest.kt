package org.gem.ui.text

import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.testing.controllerBackedAppearanceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppearanceProfileDisplayLabelTest {
    private val text = EnglishGemTextCatalogue

    @Test
    fun currentLabelUsesSelectedStockOrDefaultThemeLabel() {
        val stock = AppearanceProfileCatalogue.stockProfiles().first { it.id == AppearanceProfileId("stock-goth-dark") }
        val state = controllerBackedAppearanceState(osDark = true).copy(
            selectedProfileId = stock.id,
            stockProfiles = AppearanceProfileCatalogue.stockProfiles(),
        )

        assertEquals("Goth (Dark)", AppearanceProfileDisplayLabel.current(state, text))
        assertEquals("GEM Default (Dark)", AppearanceProfileDisplayLabel.current(state.copy(selectedProfileId = null), text))
        assertEquals(
            "GEM Default (Dark)",
            AppearanceProfileDisplayLabel.current(state.copy(selectedProfileId = AppearanceProfileId("missing")), text),
        )
    }

    @Test
    fun profileLabelRejectsHiddenSystemProfiles() {
        val system = AppearanceProfileCatalogue.systemProfile(
            mode = AppearanceMode.LIGHT,
            textFonts = AppearanceTextTarget.entries.associateWith { AppearanceFontFamily("sans-serif") },
        )
        val custom = AppearanceProfileCatalogue.stockProfiles()
            .first { it.mode == AppearanceMode.DARK }
            .copy(
                id = AppearanceProfileId("custom:dark:venue"),
                name = AppearanceProfileName("Venue"),
                source = AppearanceProfileSource.CUSTOM,
            )

        assertEquals("Venue (Dark)", AppearanceProfileDisplayLabel.profile(custom, text))
        assertFailsWith<IllegalStateException> {
            AppearanceProfileDisplayLabel.profile(system, text)
        }
    }
}
