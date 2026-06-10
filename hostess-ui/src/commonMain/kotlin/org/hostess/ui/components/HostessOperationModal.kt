package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import org.hostess.ui.design.HostessTheme

@Composable
fun HostessOperationModal(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    if (!visible) {
        return
    }

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = modifier.widthIn(max = HostessTheme.spacing.modalMaxWidth),
            shape = HostessTheme.shapes.panel,
            color = HostessTheme.colors.surfaceStrong,
            contentColor = HostessTheme.colors.ink,
        ) {
            Column(
                modifier = Modifier.padding(HostessTheme.spacing.panelPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.panelPadding),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(HostessTheme.spacing.statusPillMinHeight),
                    strokeWidth = HostessTheme.spacing.borderWidth,
                    color = HostessTheme.colors.secondary,
                )
                Text(
                    text = message,
                    style = HostessTheme.typeScale.body,
                    color = HostessTheme.colors.secondary,
                )
            }
        }
    }
}
