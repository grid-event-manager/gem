package org.gem.core.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `system profiles are hidden complete profiles`() {
        val lightFonts = completeFonts("Noto Sans")
        val darkFonts = completeFonts("Segoe UI")

        val light = AppearanceProfileCatalogue.systemProfile(AppearanceMode.LIGHT, lightFonts)
        val dark = AppearanceProfileCatalogue.systemProfile(AppearanceMode.DARK, darkFonts)

        assertEquals(AppearanceProfileId("system:light:gem-default"), light.id)
        assertEquals(AppearanceProfileId("system:dark:gem-default"), dark.id)
        assertEquals(AppearanceProfileName("GEM Default"), light.name)
        assertEquals(AppearanceProfileName("GEM Default"), dark.name)
        assertEquals(AppearanceProfileSource.SYSTEM, light.source)
        assertEquals(AppearanceProfileSource.SYSTEM, dark.source)
        assertEquals(lightFonts, light.textFonts)
        assertEquals(darkFonts, dark.textFonts)
        assertEquals(AppearanceTextTarget.entries.toSet(), light.textColors.keys)
        assertEquals(AppearanceTextTarget.entries.toSet(), dark.textColors.keys)
        assertEquals(AppearanceElementTarget.entries.toSet(), light.elementColors.keys)
        assertEquals(AppearanceElementTarget.entries.toSet(), dark.elementColors.keys)
        assertEquals("#444444", light.textColors.getValue(AppearanceTextTarget.MAIN_BODY).value)
        assertEquals("#C0C8D0", dark.textColors.getValue(AppearanceTextTarget.MAIN_BODY).value)
        assertEquals("#FFFFFF", light.elementColors.getValue(AppearanceElementTarget.PAGE_BACKGROUND).value)
        assertEquals("#2A3441", dark.elementColors.getValue(AppearanceElementTarget.PAGE_BACKGROUND).value)
    }

    @Test
    fun `system profile rejects incomplete font map`() {
        val incompleteFonts = completeFonts("Noto Sans") - AppearanceTextTarget.MAIN_BODY

        assertFailsWith<IllegalArgumentException> {
            AppearanceProfileCatalogue.systemProfile(AppearanceMode.LIGHT, incompleteFonts)
        }
    }

    @Test
    fun `stock profiles are complete without default route`() {
        AppearanceProfileCatalogue.stockProfiles().forEach { profile ->
            assertEquals(AppearanceTextTarget.entries.toSet(), profile.textFonts.keys)
            assertEquals(AppearanceTextTarget.entries.toSet(), profile.textColors.keys)
            assertEquals(AppearanceElementTarget.entries.toSet(), profile.elementColors.keys)
        }

        val princessLight = AppearanceProfileCatalogue.stockProfiles()
            .first { it.id == AppearanceProfileId("stock-princess-light") }

        assertEquals("Inter", princessLight.textFonts.getValue(AppearanceTextTarget.BACK_BUTTON).value)
        assertEquals("#8AB4C4", princessLight.textColors.getValue(AppearanceTextTarget.BACK_BUTTON).value)
    }

    private fun completeFonts(value: String): Map<AppearanceTextTarget, AppearanceFontFamily> =
        AppearanceTextTarget.entries.associateWith { AppearanceFontFamily(value) }
}
