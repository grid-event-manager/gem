package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTheme

@Composable
fun GemSegmentButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(spacing.controlHeight),
        shape = GemTheme.shapes.control,
        border = BorderStroke(
            width = spacing.borderWidth,
            color = GemSegmentButtonTokens.borderInk(colors, selected),
        ),
        contentPadding = PaddingValues(horizontal = spacing.fieldGap),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GemSegmentButtonTokens.containerFill(colors, selected),
            contentColor = GemSegmentButtonTokens.contentInk(colors),
            disabledContainerColor = colors.disabledBackground,
            disabledContentColor = colors.disabledInk,
        ),
    ) {
        Text(
            text = text,
            style = GemTheme.typeScale.button,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal object GemSegmentButtonTokens {
    fun borderInk(
        colors: GemColors,
        selected: Boolean,
    ): Color =
        if (selected) colors.primary else colors.lineStrong

    fun containerFill(
        colors: GemColors,
        selected: Boolean,
    ): Color =
        if (selected) colors.selectedBackground else colors.surfaceStrong

    fun contentInk(colors: GemColors): Color =
        colors.buttonLabelInk
}
