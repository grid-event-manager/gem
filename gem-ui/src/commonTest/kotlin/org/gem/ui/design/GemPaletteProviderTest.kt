package org.gem.ui.design

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class GemPaletteProviderTest {
    @Test
    fun lightPaletteMatchesHaccuSourceColors() {
        val colors = HaccuGemPaletteProvider.colors(ResolvedThemeMode.LIGHT)

        assertEquals(Color(0xFFFFFFFF), colors.page)
        assertEquals(Color(0xFFFAFAF8), colors.surface)
        assertEquals(Color(0xFF000000), colors.ink)
        assertEquals(Color(0xFF444444), colors.body)
        assertEquals(Color(0xFF777777), colors.muted)
        assertEquals(Color(0xFFA9D6E6), colors.brandWordmark)
        assertEquals(Color(0xFF4A7A8A), colors.brandAccent)
        assertEquals(Color(0xFF5A778C), colors.brandMark)
        assertEquals(Color(0xFFE0E0E0), colors.line)
        assertEquals(Color(0xFF8AB4C4), colors.lineStrong)
        assertEquals(Color(0xFF8AB4C4), colors.secondary)
        assertEquals(Color(0xFF243039), colors.topBar)
        assertEquals(Color(0xFF8AB4C4), colors.topBarInk)
        assertEquals(Color(0xFFFFFFFF), colors.brandInitialsInk)
        assertEquals(Color(0xFF8AB4C4), colors.topBarMenuInk)
        assertEquals(Color(0xFFFFFFFF), colors.topBarClockInk)
        assertEquals(Color(0xFF2A3441), colors.topBarButtonSurface)
        assertEquals(Color(0xFF3A4D5E), colors.topBarButtonBorder)
        assertEquals(Color(0xFF4A7A8A), colors.primary)
        assertEquals(Color(0xFF8AB4C4), colors.navigationInk)
        assertEquals(Color(0xFF8B0101), colors.interactiveHoverInk)
        assertEquals(Color(0xFFFFFFFF), colors.menuSurface)
        assertEquals(Color(0xFFB8B8B8), colors.menuDisabledInk)
        assertEquals(Color(0xFF8AB4C4), colors.fieldBorder)
        assertEquals(Color(0xFF5A778C), colors.buttonLabelInk)
        assertEquals(Color(0xFFB5544D), colors.danger)
        assertEquals(Color(0xFFA84A43), colors.dangerFill)
    }

    @Test
    fun darkPaletteMatchesHaccuSourceColors() {
        val colors = HaccuGemPaletteProvider.colors(ResolvedThemeMode.DARK)

        assertEquals(Color(0xFF2A3441), colors.page)
        assertEquals(Color(0xFF243039), colors.surface)
        assertEquals(Color(0xFF8AB4C4), colors.ink)
        assertEquals(Color(0xFFC0C8D0), colors.body)
        assertEquals(Color(0xFFA0B0BC), colors.muted)
        assertEquals(Color(0xFF8AB4C4), colors.secondary)
        assertEquals(Color(0xFFA9D6E6), colors.brandWordmark)
        assertEquals(Color(0xFFA84A43), colors.dangerFill)
        assertEquals(Color(0xFF4A7A8A), colors.brandAccent)
        assertEquals(Color(0xFF4A7A8A), colors.brandMark)
        assertEquals(Color(0xFF2E3F4E), colors.line)
        assertEquals(Color(0xFF1A2530), colors.statusBackground)
        assertEquals(Color(0xFF1E2A35), colors.menuHover)
        assertEquals(Color(0xFF222E3A), colors.menuActive)
        assertEquals(Color(0xFFA0B0BC), colors.menuDisabledInk)
        assertEquals(Color(0xFF2F3D4A), colors.surfaceStrong)
        assertEquals(Color(0xFF2A3441), colors.fieldSurface)
        assertEquals(Color(0xFF3A4D5E), colors.fieldBorder)
        assertEquals(Color(0xFF2A3441), colors.menuSurface)
        assertEquals(Color(0xFF2E3F4E), colors.toggleTrack)
        assertEquals(Color(0xFFFFFFFF), colors.brandInitialsInk)
        assertEquals(Color(0xFF8AB4C4), colors.topBarMenuInk)
        assertEquals(Color(0xFFFFFFFF), colors.topBarClockInk)
        assertEquals(Color(0xFF2A3441), colors.topBarButtonSurface)
        assertEquals(Color(0xFF3A4D5E), colors.topBarButtonBorder)
        assertEquals(Color(0xFF8AB4C4), colors.navigationInk)
        assertEquals(Color(0xFFC0C8D0), colors.interactiveHoverInk)
        assertEquals(Color(0xFF8AB4C4), colors.buttonLabelInk)
    }

    @Test
    fun darkPaletteTextTokensAvoidBrightWhiteExceptClock() {
        val colors = HaccuGemPaletteProvider.colors(ResolvedThemeMode.DARK)
        val brightWhite = Color(0xFFFFFFFF)

        assertNotEquals(brightWhite, colors.ink)
        assertNotEquals(brightWhite, colors.body)
        assertNotEquals(brightWhite, colors.primaryInk)
        assertNotEquals(brightWhite, colors.secondary)
        assertNotEquals(brightWhite, colors.selectedInk)
        assertNotEquals(brightWhite, colors.topBarInk)
        assertNotEquals(brightWhite, colors.brandMarkInk)
        assertNotEquals(brightWhite, colors.buttonLabelInk)
    }
}
