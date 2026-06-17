package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceColorParseResult
import org.gem.ui.design.GemTheme
import org.gem.ui.design.toComposeColor
import org.gem.ui.state.AppearanceRgbChannel

@Composable
fun GemColorPicker(
    selectedColor: AppearanceColor,
    onSwatchSelected: (AppearanceColor) -> Unit,
    onRgbValueChanged: (AppearanceRgbChannel, Int) -> Unit,
    onRgbInputInvalid: (AppearanceRgbChannel) -> Unit,
    onHexChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    invalidRgbChannels: Set<AppearanceRgbChannel> = emptySet(),
    hexInputInvalid: Boolean = false,
) {
    val spacing = GemTheme.spacing
    val channels = GemColorPickerInteraction.channels(selectedColor)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.appearanceColorPickerGap),
    ) {
        GemSwatchGrid(onSwatchSelected = onSwatchSelected)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.appearanceRgbToneGap),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.appearanceRgbRowGap),
            ) {
                AppearanceRgbChannel.entries.forEach { channel ->
                    GemRgbSliderRow(
                        channel = channel,
                        value = channels.getValue(channel),
                        onValueChange = onRgbValueChanged,
                        invalid = channel in invalidRgbChannels,
                        onInvalidInput = onRgbInputInvalid,
                    )
                }
            }
            Column(
                modifier = Modifier.width(spacing.appearanceTonePreviewWidth),
                verticalArrangement = Arrangement.spacedBy(spacing.fieldGap),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spacing.appearanceTonePreviewHeight),
                    shape = GemTheme.shapes.compactControl,
                    color = selectedColor.toComposeColor(),
                    border = BorderStroke(spacing.borderWidth, GemTheme.colors.fieldBorder),
                ) {
                }
                GemCompactTextField(
                    value = selectedColor.value,
                    onValueChange = onHexChanged,
                    invalid = hexInputInvalid,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

internal object GemColorPickerInteraction {
    fun channels(color: AppearanceColor): Map<AppearanceRgbChannel, Int> {
        val rgb = color.value.removePrefix("#")
        return mapOf(
            AppearanceRgbChannel.R to rgb.substring(0, 2).toInt(radix = 16),
            AppearanceRgbChannel.G to rgb.substring(2, 4).toInt(radix = 16),
            AppearanceRgbChannel.B to rgb.substring(4, 6).toInt(radix = 16),
        )
    }

    fun hexUpdate(rawValue: String): AppearanceColorParseResult =
        AppearanceColor.from(rawValue)
}
