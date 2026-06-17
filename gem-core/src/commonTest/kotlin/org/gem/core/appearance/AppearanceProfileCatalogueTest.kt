package org.gem.core.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppearanceProfileCatalogueTest {
    @Test
    fun `stock catalogue contains exactly the six prototype profiles in order`() {
        val profiles = AppearanceProfileCatalogue.stockProfiles()

        assertEquals(
            listOf(
                "stock-princess-light",
                "stock-princess-dark",
                "stock-goth-light",
                "stock-goth-dark",
                "stock-cyber-light",
                "stock-cyber-dark",
            ),
            profiles.map { it.id.value },
        )
        assertTrue(profiles.all { it.source == AppearanceProfileSource.STOCK })
    }

    @Test
    fun `stock profile preserves prototype overrides and brand diamond as logo colour`() {
        val princessLight = AppearanceProfileCatalogue.stockProfiles()
            .first { it.id == AppearanceProfileId("stock-princess-light") }
        val cyberDark = AppearanceProfileCatalogue.stockProfiles()
            .first { it.id == AppearanceProfileId("stock-cyber-dark") }

        assertEquals(AppearanceProfileName("Princess"), princessLight.name)
        assertEquals(AppearanceMode.LIGHT, princessLight.mode)
        assertEquals("Georgia", princessLight.textFonts.getValue(AppearanceTextTarget.TITLE_BAR).value)
        assertEquals("Trebuchet MS", princessLight.textFonts.getValue(AppearanceTextTarget.BUTTON_LABELS).value)
        assertEquals("#B85699", princessLight.textColors.getValue(AppearanceTextTarget.BUTTON_LABELS).value)
        assertEquals("#FFFFFF", princessLight.textColors.getValue(AppearanceTextTarget.LOGO).value)
        assertEquals("#F1C7E6", princessLight.elementColors.getValue(AppearanceElementTarget.HAMBURGER_BACKGROUND).value)

        assertEquals(AppearanceProfileName("Cyber"), cyberDark.name)
        assertEquals(AppearanceMode.DARK, cyberDark.mode)
        assertEquals("Courier New", cyberDark.textFonts.getValue(AppearanceTextTarget.SLT_CLOCK).value)
        assertEquals("#00E5FF", cyberDark.textColors.getValue(AppearanceTextTarget.LOGO).value)
        assertEquals("#071720", cyberDark.elementColors.getValue(AppearanceElementTarget.PAGE_BACKGROUND).value)
    }

    @Test
    fun `default draft covers every target without selected profile`() {
        val draft = AppearanceProfileCatalogue.defaultDraft(AppearanceMode.DARK)

        assertEquals(AppearanceMode.DARK, draft.mode)
        assertEquals(AppearanceTextTarget.entries.toSet(), draft.textFonts.keys)
        assertEquals(AppearanceTextTarget.entries.toSet(), draft.textColors.keys)
        assertEquals(AppearanceElementTarget.entries.toSet(), draft.elementColors.keys)
        assertNull(draft.selectedProfileId)
        assertEquals(false, draft.dirty)
        assertEquals("#C0C8D0", draft.textColors.getValue(AppearanceTextTarget.MAIN_BODY).value)
    }
}
