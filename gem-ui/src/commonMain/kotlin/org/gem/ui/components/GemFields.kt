package org.gem.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTheme
import org.gem.ui.design.GemTypeScale

@Composable
fun GemTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: Dp? = null,
    maxHeight: Dp? = null,
) {
    val spacing = GemTheme.spacing
    val textFieldModifier = Modifier
        .fillMaxWidth()
        .then(
            if (minHeight == null && maxHeight == null) {
                Modifier.height(spacing.controlHeight)
            } else {
                Modifier.heightIn(
                    min = minHeight ?: spacing.controlHeight,
                    max = maxHeight ?: Dp.Infinity,
                )
            },
        )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.fieldGap),
    ) {
        GemFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 5,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            textStyle = GemFieldTokens.inputTextStyle(GemTheme.typeScale),
            shape = GemTheme.shapes.control,
            colors = gemTextFieldColors(),
            modifier = textFieldModifier,
        )
    }
}

@Composable
fun GemPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    revealText: String,
    hideText: String,
    revealed: Boolean,
    onRevealChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onPasswordFocus: () -> Unit = {},
) {
    val spacing = GemTheme.spacing
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.fieldGap),
    ) {
        GemFieldLabel(label)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.fieldGap)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = GemFieldTokens.inputTextStyle(GemTheme.typeScale),
                shape = GemTheme.shapes.control,
                visualTransformation = if (revealed) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                colors = gemTextFieldColors(),
                modifier = Modifier
                    .weight(weight = 1f)
                    .height(spacing.controlHeight)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onPasswordFocus()
                        }
                    },
            )
            GemPlainButton(
                text = if (revealed) hideText else revealText,
                onClick = { onRevealChanged(!revealed) },
                enabled = enabled,
                modifier = Modifier.width(spacing.passwordRevealButtonWidth),
            )
        }
    }
}

@Composable
fun GemFieldLabel(label: String) {
    Text(
        text = label,
        color = GemFieldTokens.labelColor(GemTheme.colors),
        style = GemFieldTokens.labelStyle(GemTheme.typeScale),
    )
}

internal object GemFieldTokens {
    fun labelColor(colors: GemColors): Color =
        colors.muted

    fun labelStyle(typeScale: GemTypeScale): TextStyle =
        typeScale.smallLabel

    fun inputTextStyle(typeScale: GemTypeScale): TextStyle =
        typeScale.fieldText
}

@Composable
fun GemCompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    invalid: Boolean = false,
    textAlign: TextAlign = TextAlign.Center,
    placeholder: String? = null,
    onFocusedChange: (Boolean) -> Unit = {},
) {
    val colors = GemTheme.colors
    val typeScale = GemTheme.typeScale
    Surface(
        modifier = modifier
            .height(GemTheme.spacing.appearanceCompactFieldHeight)
            .onFocusChanged { focusState -> onFocusedChange(focusState.isFocused) },
        shape = GemTheme.shapes.compactControl,
        color = colors.fieldSurface,
        contentColor = GemCompactFieldTokens.textColor(colors, enabled, invalid),
        border = BorderStroke(
            GemTheme.spacing.borderWidth,
            GemCompactFieldTokens.borderColor(colors, enabled, invalid),
        ),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = GemCompactFieldTokens.textStyle(
                typeScale = typeScale,
                color = GemCompactFieldTokens.textColor(colors, enabled, invalid),
                textAlign = textAlign,
            ),
            cursorBrush = SolidColor(GemCompactFieldTokens.cursorColor(colors, invalid)),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = GemTheme.spacing.appearanceCompactFieldHorizontalPadding),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = GemCompactFieldTokens.contentAlignment(textAlign),
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = GemCompactFieldTokens.textStyle(
                                typeScale = typeScale,
                                color = GemCompactFieldTokens.placeholderColor(colors),
                                textAlign = textAlign,
                            ),
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

internal object GemCompactFieldTokens {
    fun textColor(
        colors: GemColors,
        enabled: Boolean,
        invalid: Boolean,
    ): Color =
        when {
            invalid -> colors.danger
            enabled -> colors.ink
            else -> colors.disabledInk
        }

    fun placeholderColor(colors: GemColors): Color =
        colors.muted

    fun borderColor(
        colors: GemColors,
        enabled: Boolean,
        invalid: Boolean,
    ): Color =
        when {
            invalid -> colors.danger
            enabled -> colors.fieldBorder
            else -> colors.line
        }

    fun cursorColor(
        colors: GemColors,
        invalid: Boolean,
    ): Color =
        if (invalid) colors.danger else colors.primary

    fun textStyle(
        typeScale: GemTypeScale,
        color: Color,
        textAlign: TextAlign,
    ): TextStyle =
        typeScale.smallLabel.copy(
            color = color,
            textAlign = textAlign,
        )

    fun contentAlignment(textAlign: TextAlign): Alignment =
        when (textAlign) {
            TextAlign.Start,
            TextAlign.Left,
            -> Alignment.CenterStart
            TextAlign.End,
            TextAlign.Right,
            -> Alignment.CenterEnd
            else -> Alignment.Center
        }
}

@Composable
fun gemTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GemTheme.colors.ink,
    unfocusedTextColor = GemTheme.colors.ink,
    disabledTextColor = GemTheme.colors.disabledInk,
    focusedPlaceholderColor = GemTheme.colors.muted,
    unfocusedPlaceholderColor = GemTheme.colors.muted,
    focusedLabelColor = GemTheme.colors.muted,
    unfocusedLabelColor = GemTheme.colors.muted,
    focusedContainerColor = GemTheme.colors.fieldSurface,
    unfocusedContainerColor = GemTheme.colors.fieldSurface,
    disabledContainerColor = GemTheme.colors.fieldSurface,
    focusedBorderColor = GemTheme.colors.fieldBorder,
    unfocusedBorderColor = GemTheme.colors.fieldBorder,
    disabledBorderColor = GemTheme.colors.line,
    errorTextColor = GemTheme.colors.danger,
    errorBorderColor = GemTheme.colors.danger,
    errorCursorColor = GemTheme.colors.danger,
    cursorColor = GemTheme.colors.primary,
)
