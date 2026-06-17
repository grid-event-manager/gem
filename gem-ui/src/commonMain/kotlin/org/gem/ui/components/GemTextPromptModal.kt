package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import org.gem.ui.design.GemTheme

@Composable
fun GemTextPromptModal(
    visible: Boolean,
    name: String,
    placeholder: String,
    lightLabel: String,
    darkLabel: String,
    checked: Boolean,
    cancelText: String,
    saveText: String,
    onNameChange: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    nameInvalid: Boolean = false,
) {
    if (!visible) {
        return
    }

    val spacing = GemTheme.spacing
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = modifier.widthIn(max = spacing.modalMaxWidth),
            shape = GemTheme.shapes.panel,
            color = GemTheme.colors.surfaceStrong,
            contentColor = GemTheme.colors.body,
        ) {
            Column(
                modifier = Modifier.padding(spacing.appearanceModalPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.appearanceModalGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ThemeModeToggle(
                    checked = checked,
                    lightLabel = lightLabel,
                    darkLabel = darkLabel,
                    onCheckedChange = onCheckedChange,
                )
                GemCompactTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = placeholder,
                    invalid = nameInvalid,
                    textAlign = TextAlign.Start,
                    onFocusedChange = { focused ->
                        val nextName = if (focused) {
                            GemTextPromptModalInteraction.focusedValue(name, placeholder)
                        } else {
                            GemTextPromptModalInteraction.blurredValue(name, placeholder)
                        }
                        if (nextName != name) {
                            onNameChange(nextName)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.appearanceProfileActionGap),
                ) {
                    GemSecondaryButton(
                        text = cancelText,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                    GemSecondaryButton(
                        text = saveText,
                        onClick = onSave,
                        enabled = GemTextPromptModalInteraction.canSave(name, placeholder),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

internal object GemTextPromptModalInteraction {
    val contentOrder: List<String> = listOf("theme-toggle", "name-field", "actions")
    val actionOrder: List<String> = listOf("cancel", "save")

    fun canSave(
        name: String,
        placeholder: String,
    ): Boolean =
        name.isNotBlank() && name != placeholder

    fun focusedValue(
        value: String,
        placeholder: String,
    ): String =
        if (value == placeholder) "" else value

    fun blurredValue(
        value: String,
        placeholder: String,
    ): String =
        value.ifBlank { placeholder }
}
