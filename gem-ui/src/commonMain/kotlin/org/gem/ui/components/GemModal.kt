package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import org.gem.ui.design.GemTheme

@Composable
fun GemConfirmModal(
    visible: Boolean,
    title: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    confirmModifier: Modifier = Modifier,
    cancelModifier: Modifier = Modifier,
) {
    if (!visible) {
        return
    }

    val colors = GemTheme.colors
    val spacing = GemTheme.spacing
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = modifier.widthIn(max = spacing.modalMaxWidth),
            shape = GemTheme.shapes.panel,
            color = colors.surfaceStrong,
            contentColor = colors.ink,
        ) {
            Column(
                modifier = Modifier.padding(spacing.panelPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.panelPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    color = colors.ink,
                    style = GemTheme.typeScale.sectionTitle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.rowGap, Alignment.CenterHorizontally),
                ) {
                    GemSecondaryButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = spacing.modalActionMinWidth)
                            .then(confirmModifier),
                    )
                    GemSecondaryButton(
                        text = cancelText,
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = spacing.modalActionMinWidth)
                            .then(cancelModifier),
                    )
                }
            }
        }
    }
}
