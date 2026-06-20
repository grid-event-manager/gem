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
        assertEquals("#8AB4C4", light.elementColors.getValue(AppearanceElementTarget.ACCENT_TEXT).value)
        assertEquals("#B5544D", light.elementColors.getValue(AppearanceElementTarget.ERROR_TEXT).value)
        assertEquals("#4A7A8A", light.elementColors.getValue(AppearanceElementTarget.STATUS_TEXT).value)
        assertEquals("#B8B8B8", light.elementColors.getValue(AppearanceElementTarget.MENU_DISABLED_TEXT).value)
        assertEquals("#8B0101", light.elementColors.getValue(AppearanceElementTarget.INTERACTIVE_HOVER_TEXT).value)
        assertEquals("#8AB4C4", dark.elementColors.getValue(AppearanceElementTarget.ACCENT_TEXT).value)
        assertEquals("#B5544D", dark.elementColors.getValue(AppearanceElementTarget.ERROR_TEXT).value)
        assertEquals("#8AB4C4", dark.elementColors.getValue(AppearanceElementTarget.STATUS_TEXT).value)
        assertEquals("#A0B0BC", dark.elementColors.getValue(AppearanceElementTarget.MENU_DISABLED_TEXT).value)
        assertEquals("#C0C8D0", dark.elementColors.getValue(AppearanceElementTarget.INTERACTIVE_HOVER_TEXT).value)
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
        assertEquals("#B85699", princessLight.textColors.getValue(AppearanceTextTarget.BACK_BUTTON).value)
        assertEquals("#B85699", princessLight.elementColors.getValue(AppearanceElementTarget.ACCENT_TEXT).value)
        assertEquals("#B5544D", princessLight.elementColors.getValue(AppearanceElementTarget.ERROR_TEXT).value)
        assertEquals("#B85699", princessLight.elementColors.getValue(AppearanceElementTarget.STATUS_TEXT).value)
        assertEquals("#8A6B82", princessLight.elementColors.getValue(AppearanceElementTarget.MENU_DISABLED_TEXT).value)
        assertEquals("#B85699", princessLight.elementColors.getValue(AppearanceElementTarget.INTERACTIVE_HOVER_TEXT).value)
    }

    @Test
    fun `stock profiles own semantic element colours`() {
        val expected = mapOf(
            "stock-princess-light" to StockSemanticColours("#B85699", "#B85699", "#8A6B82", "#B85699", "#B5544D"),
            "stock-princess-dark" to StockSemanticColours("#FF9EDB", "#FF9EDB", "#ACC3D8", "#BFE9FF", "#B5544D"),
            "stock-goth-light" to StockSemanticColours("#6C284F", "#6C284F", "#745B70", "#6C284F", "#B5544D"),
            "stock-goth-dark" to StockSemanticColours("#D94F8C", "#D94F8C", "#A991A8", "#D94F8C", "#B5544D"),
            "stock-cyber-light" to StockSemanticColours("#006F86", "#006F86", "#53717A", "#006F86", "#B5544D"),
            "stock-cyber-dark" to StockSemanticColours("#00E5FF", "#00E5FF", "#83B5BF", "#00E5FF", "#B5544D"),
        )

        val profiles = AppearanceProfileCatalogue.stockProfiles().associateBy { it.id.value }

        expected.forEach { (profileId, colours) ->
            val profile = profiles.getValue(profileId)

            assertEquals(colours.accent, profile.elementColors.getValue(AppearanceElementTarget.ACCENT_TEXT).value)
            assertEquals(colours.status, profile.elementColors.getValue(AppearanceElementTarget.STATUS_TEXT).value)
            assertEquals(
                colours.menuDisabled,
                profile.elementColors.getValue(AppearanceElementTarget.MENU_DISABLED_TEXT).value,
            )
            assertEquals(
                colours.hover,
                profile.elementColors.getValue(AppearanceElementTarget.INTERACTIVE_HOVER_TEXT).value,
            )
            assertEquals(colours.error, profile.elementColors.getValue(AppearanceElementTarget.ERROR_TEXT).value)
        }
    }

    @Test
    fun `stock profiles copy theme toggle colour to omitted back colour`() {
        val profiles = AppearanceProfileCatalogue.stockProfiles().associateBy { it.id.value }
        val omittedBackProfileIds = listOf(
            "stock-princess-light",
            "stock-goth-light",
            "stock-goth-dark",
            "stock-cyber-light",
            "stock-cyber-dark",
        )

        for (profileId in omittedBackProfileIds) {
            val profile = profiles.getValue(profileId)

            assertEquals(
                profile.textColors.getValue(AppearanceTextTarget.THEME_TOGGLE_LABELS),
                profile.textColors.getValue(AppearanceTextTarget.BACK_BUTTON),
            )
        }

        assertEquals(
            "#BFE9FF",
            profiles.getValue("stock-princess-dark")
                .textColors
                .getValue(AppearanceTextTarget.BACK_BUTTON)
                .value,
        )
    }

    @Test
    fun `profile completion fills only stock and saved custom compatibility gaps`() {
        val completedFonts = AppearanceProfileCompletion.completeTextFonts(
            mapOf(AppearanceTextTarget.TITLE_BAR to AppearanceFontFamily("Display")),
        )
        val completedElements = AppearanceProfileCompletion.completeElementColors(
            AppearanceMode.DARK,
            mapOf(AppearanceElementTarget.PAGE_BACKGROUND to AppearanceColor.require("#123456")),
        )

        assertEquals("Display", completedFonts.getValue(AppearanceTextTarget.TITLE_BAR).value)
        assertEquals("Inter", completedFonts.getValue(AppearanceTextTarget.MAIN_BODY).value)
        assertEquals("#123456", completedElements.getValue(AppearanceElementTarget.PAGE_BACKGROUND).value)
        assertEquals("#C0C8D0", completedElements.getValue(AppearanceElementTarget.INTERACTIVE_HOVER_TEXT).value)
    }

    private fun completeFonts(value: String): Map<AppearanceTextTarget, AppearanceFontFamily> =
        AppearanceTextTarget.entries.associateWith { AppearanceFontFamily(value) }

    private data class StockSemanticColours(
        val accent: String,
        val status: String,
        val menuDisabled: String,
        val hover: String,
        val error: String,
    )
}
