package org.gem.ui.components

import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceColorParseResult
import org.gem.ui.state.AppearanceRgbChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GemColorPickerTest {
    @Test
    fun channelsComeFromSelectedColour() {
        val channels = GemColorPickerInteraction.channels(AppearanceColor.require("#1234AB"))

        assertEquals(0x12, channels.getValue(AppearanceRgbChannel.R))
        assertEquals(0x34, channels.getValue(AppearanceRgbChannel.G))
        assertEquals(0xAB, channels.getValue(AppearanceRgbChannel.B))
    }

    @Test
    fun rgbNumericInputRestoresPreviousValidValueOnInvalidInput() {
        assertEquals(GemRgbNumericUpdate.Valid(17, "17"), GemRgbSliderRowInteraction.numericUpdate(42, "17"))
        assertEquals(GemRgbNumericUpdate.Invalid("42"), GemRgbSliderRowInteraction.numericUpdate(42, "300"))
        assertEquals(GemRgbNumericUpdate.Invalid("42"), GemRgbSliderRowInteraction.numericUpdate(42, "nope"))
    }

    @Test
    fun hexInputUsesAppearanceColourValidation() {
        val valid = assertIs<AppearanceColorParseResult.Valid>(GemColorPickerInteraction.hexUpdate("abc"))
        val invalid = GemColorPickerInteraction.hexUpdate("not-a-colour")

        assertEquals("#AABBCC", valid.color.value)
        assertIs<AppearanceColorParseResult.Invalid>(invalid)
    }
}
