package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.gem.ui.design.GemTheme
import org.gem.ui.state.AppearanceRgbChannel
import kotlin.math.roundToInt

@Composable
fun GemRgbSliderRow(
    channel: AppearanceRgbChannel,
    value: Int,
    onValueChange: (AppearanceRgbChannel, Int) -> Unit,
    modifier: Modifier = Modifier,
    invalid: Boolean = false,
    onInvalidInput: (AppearanceRgbChannel) -> Unit = {},
) {
    val spacing = GemTheme.spacing
    var fieldValue by remember(value) { mutableStateOf(value.coerceIn(GemRgbSliderRowInteraction.Range).toString()) }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.appearanceRgbRowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = channel.name,
            color = GemTheme.colors.secondary,
            style = GemTheme.typeScale.smallLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(spacing.appearanceRgbLabelWidth),
        )
        Slider(
            value = value.coerceIn(GemRgbSliderRowInteraction.Range).toFloat(),
            onValueChange = {
                val next = it.roundToInt().coerceIn(GemRgbSliderRowInteraction.Range)
                fieldValue = next.toString()
                onValueChange(channel, next)
            },
            valueRange = GemRgbSliderRowInteraction.Range.first.toFloat()..GemRgbSliderRowInteraction.Range.last.toFloat(),
            steps = GemRgbSliderRowInteraction.Steps,
            colors = SliderDefaults.colors(
                thumbColor = GemTheme.colors.primary,
                activeTrackColor = GemTheme.colors.primary,
                inactiveTrackColor = GemTheme.colors.lineStrong,
            ),
            modifier = Modifier.weight(1f),
        )
        GemCompactTextField(
            value = fieldValue,
            onValueChange = { raw ->
                when (val update = GemRgbSliderRowInteraction.numericUpdate(value, raw)) {
                    is GemRgbNumericUpdate.Valid -> {
                        fieldValue = update.displayValue
                        onValueChange(channel, update.value)
                    }
                    is GemRgbNumericUpdate.Invalid -> {
                        fieldValue = update.restoredDisplayValue
                        onInvalidInput(channel)
                    }
                }
            },
            invalid = invalid,
            modifier = Modifier.width(spacing.appearanceRgbFieldWidth),
        )
    }
}

sealed interface GemRgbNumericUpdate {
    data class Valid(val value: Int, val displayValue: String) : GemRgbNumericUpdate
    data class Invalid(val restoredDisplayValue: String) : GemRgbNumericUpdate
}

internal object GemRgbSliderRowInteraction {
    val Range: IntRange = 0..255
    const val Steps: Int = 254

    fun numericUpdate(
        previousValidValue: Int,
        rawValue: String,
    ): GemRgbNumericUpdate {
        val parsed = rawValue.toIntOrNull()
        return if (parsed != null && parsed in Range) {
            GemRgbNumericUpdate.Valid(parsed, parsed.toString())
        } else {
            GemRgbNumericUpdate.Invalid(previousValidValue.coerceIn(Range).toString())
        }
    }
}
