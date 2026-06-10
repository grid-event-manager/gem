package org.hostess.ui.components

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
import androidx.compose.ui.window.Dialog
import org.hostess.ui.design.HostessTheme

@Composable
fun HostessConfirmModal(
    visible: Boolean,
    title: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) {
        return
    }

    val colors = HostessTheme.colors
    val spacing = HostessTheme.spacing
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = modifier.widthIn(max = spacing.modalMaxWidth),
            shape = HostessTheme.shapes.panel,
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
                    style = HostessTheme.typeScale.sectionTitle,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.rowGap, Alignment.CenterHorizontally),
                ) {
                    HostessSecondaryButton(
                        text = confirmText,
                        onClick = onConfirm,
                    )
                    HostessSecondaryButton(
                        text = cancelText,
                        onClick = onCancel,
                    )
                }
            }
        }
    }
}
