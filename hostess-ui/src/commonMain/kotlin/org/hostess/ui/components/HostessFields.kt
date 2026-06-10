package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import org.hostess.ui.design.HostessTheme

@Composable
fun HostessTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minHeight: Dp? = null,
    maxHeight: Dp? = null,
) {
    val spacing = HostessTheme.spacing
    val textFieldModifier = Modifier
        .fillMaxWidth()
        .heightIn(
            min = minHeight ?: spacing.tapTarget,
            max = maxHeight ?: Dp.Infinity,
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
            textStyle = HostessTheme.typeScale.body,
            shape = HostessTheme.shapes.control,
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
    val spacing = HostessTheme.spacing
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
                textStyle = HostessTheme.typeScale.body,
                shape = HostessTheme.shapes.control,
                visualTransformation = if (revealed) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                colors = hostessTextFieldColors(),
                modifier = Modifier
                    .weight(weight = 1f)
                    .heightIn(min = spacing.tapTarget)
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
            )
        }
    }
}

@Composable
fun HostessFieldLabel(label: String) {
    Text(
        text = label,
        color = HostessTheme.colors.muted,
        style = HostessTheme.typeScale.smallLabel,
    )
}

@Composable
fun hostessTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = HostessTheme.colors.ink,
    unfocusedTextColor = HostessTheme.colors.ink,
    disabledTextColor = HostessTheme.colors.disabledInk,
    focusedContainerColor = HostessTheme.colors.surfaceStrong,
    unfocusedContainerColor = HostessTheme.colors.surfaceStrong,
    disabledContainerColor = HostessTheme.colors.statusBackground,
    focusedBorderColor = HostessTheme.colors.primary,
    unfocusedBorderColor = HostessTheme.colors.lineStrong,
    disabledBorderColor = HostessTheme.colors.line,
    cursorColor = HostessTheme.colors.primary,
)
