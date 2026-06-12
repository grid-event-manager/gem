package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import org.gem.ui.design.GemTheme

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
        HostessFieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 5,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            textStyle = GemTheme.typeScale.body,
            shape = GemTheme.shapes.control,
            colors = hostessTextFieldColors(),
            modifier = textFieldModifier,
        )
    }
}

@Composable
fun HostessPasswordField(
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
        HostessFieldLabel(label)
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.fieldGap)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = GemTheme.typeScale.body,
                shape = GemTheme.shapes.control,
                visualTransformation = if (revealed) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                colors = hostessTextFieldColors(),
                modifier = Modifier
                    .weight(weight = 1f)
                    .height(spacing.controlHeight)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onPasswordFocus()
                        }
                    },
            )
            HostessPlainButton(
                text = if (revealed) hideText else revealText,
                onClick = { onRevealChanged(!revealed) },
                enabled = enabled,
                modifier = Modifier.width(spacing.passwordRevealButtonWidth),
            )
        }
    }
}

@Composable
fun HostessFieldLabel(label: String) {
    Text(
        text = label,
        color = GemTheme.colors.secondary,
        style = GemTheme.typeScale.smallLabel,
    )
}

@Composable
fun hostessTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
    cursorColor = GemTheme.colors.primary,
)
