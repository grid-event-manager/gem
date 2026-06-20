package org.gem.ui.components

import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceColorParseResult
import org.gem.ui.design.GemShapes
import org.gem.ui.design.GemSpacing
import org.gem.ui.state.AppearanceRgbChannel
import androidx.compose.ui.unit.dp
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
    fun whiteHexChannelsNumericFieldsAndToneUseOneSelectedColour() {
        val selected = AppearanceColor.require("#FFFFFF")
        val channels = GemColorPickerInteraction.channels(selected)

        assertEquals(255, channels.getValue(AppearanceRgbChannel.R))
        assertEquals(255, channels.getValue(AppearanceRgbChannel.G))
        assertEquals(255, channels.getValue(AppearanceRgbChannel.B))
        assertEquals("#FFFFFF", GemColorPickerInteraction.hexDisplay(selected))
        assertEquals(selected, AppearanceColor.require(GemColorPickerInteraction.hexDisplay(selected)))
    }

    @Test
    fun selectedColourRefreshesHexAndRgbFeedbackTogether() {
        val first = GemColorPickerFeedback.from(AppearanceColor.require("#FFFFFF"))
        val second = GemColorPickerFeedback.from(AppearanceColor.require("#008AFF"))

        assertEquals(GemColorPickerFeedback("#FFFFFF", 255, 255, 255), first)
        assertEquals(GemColorPickerFeedback("#008AFF", 0, 138, 255), second)
    }

    @Test
    fun hexDisplayAlwaysUsesUppercaseCanonicalForm() {
        assertEquals("#008AFF", GemColorPickerInteraction.hexDisplay(AppearanceColor.require("#008aff")))
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

    @Test
    fun pickerGeometryUsesCentralPrototypeTokens() {
        val spacing = GemSpacing()
        val shapes = GemShapes()

        assertEquals(12.dp, spacing.appearanceColorPickerGap)
        assertEquals(12.dp, spacing.appearanceRgbToneGap)
        assertEquals(8.dp, spacing.appearanceRgbRowGap)
        assertEquals(16.dp, spacing.appearanceRgbLabelWidth)
        assertEquals(44.dp, spacing.appearanceRgbFieldWidth)
        assertEquals(104.dp, spacing.appearanceTonePreviewWidth)
        assertEquals(64.dp, spacing.appearanceTonePreviewHeight)
        assertEquals(28.dp, spacing.appearanceCompactFieldHeight)
        assertEquals(4.dp, spacing.appearanceCompactFieldHorizontalPadding)
        assertEquals(6.dp, shapes.compactControlRadius)
    }

    private data class GemColorPickerFeedback(
        val hex: String,
        val red: Int,
        val green: Int,
        val blue: Int,
    ) {
        companion object {
            fun from(color: AppearanceColor): GemColorPickerFeedback {
                val channels = GemColorPickerInteraction.channels(color)
                return GemColorPickerFeedback(
                    hex = GemColorPickerInteraction.hexDisplay(color),
                    red = channels.getValue(AppearanceRgbChannel.R),
                    green = channels.getValue(AppearanceRgbChannel.G),
                    blue = channels.getValue(AppearanceRgbChannel.B),
                )
            }
        }
    }
}
