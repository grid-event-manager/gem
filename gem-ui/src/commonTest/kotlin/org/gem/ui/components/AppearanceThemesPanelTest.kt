package org.gem.ui.components

import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.core.appearance.AppearanceTextTarget
import org.gem.ui.testing.controllerBackedAppearanceState
import org.gem.ui.text.AppearanceProfileDisplayLabel
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AppearanceThemesPanelTest {
    private val text = EnglishGemTextCatalogue

    @Test
    fun themesPanelContainsOnlyThemeDropdown() {
        assertEquals(listOf("theme-dropdown"), AppearanceThemesPanelInteraction.contentOrder)
        assertFalse(AppearanceThemesPanelInteraction.hasSaveOrResetControls())
    }

    @Test
    fun profileDropdownStartsWithChooseThemeAndListsAllProfiles() {
        val stock = AppearanceProfileCatalogue.stockProfiles()
        val custom = customProfile()
        val state = controllerBackedAppearanceState(osDark = false).copy(
            stockProfiles = stock,
            customProfiles = listOf(custom),
        )
        val options = AppearanceThemesPanelInteraction.profileOptions(state, text)

        assertEquals(text.text(GemTextKey.ChooseTheme), options.first().label)
        assertEquals(null, options.first().value)
        assertFalse(options.first().enabled)
        assertEquals(stock.size + 2, options.size)
        assertEquals(custom.id, options.last().value)
    }

    @Test
    fun stockAndCustomProfileLabelsMatchContract() {
        val stock = AppearanceProfileCatalogue.stockProfiles().first { it.mode == AppearanceMode.LIGHT }
        val custom = customProfile()

        assertEquals("${stock.name.value} ${text.text(GemTextKey.Light)}", AppearanceProfileDisplayLabel.profile(stock, text))
        assertEquals("My Theme (${text.text(GemTextKey.Dark)})", AppearanceProfileDisplayLabel.profile(custom, text))
    }

    @Test
    fun systemProfileLabelIsNonRenderingGuard() {
        val system = AppearanceProfileCatalogue.systemProfile(
            mode = AppearanceMode.LIGHT,
            textFonts = completeFonts("Noto Sans"),
        )

        assertFailsWith<IllegalStateException> {
            AppearanceProfileDisplayLabel.profile(system, text)
        }
    }

    private fun customProfile(): AppearanceProfile {
        val base = AppearanceProfileCatalogue.stockProfiles().first { it.mode == AppearanceMode.DARK }
        return base.copy(
            id = AppearanceProfileId("my-theme-dark"),
            name = AppearanceProfileName("My Theme"),
            source = AppearanceProfileSource.CUSTOM,
        )
    }

    private fun completeFonts(value: String): Map<AppearanceTextTarget, AppearanceFontFamily> =
        AppearanceTextTarget.entries.associateWith { AppearanceFontFamily(value) }
}
