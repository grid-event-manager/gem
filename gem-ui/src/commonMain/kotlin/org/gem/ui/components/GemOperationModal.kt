package org.gem.ui.components

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
import org.gem.ui.design.GemTheme

@Composable
fun GemOperationModal(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
) {
    if (!visible) {
        return
    }

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = modifier.widthIn(max = GemTheme.spacing.modalMaxWidth),
            shape = GemTheme.shapes.panel,
            color = GemTheme.colors.surfaceStrong,
            contentColor = GemTheme.colors.ink,
        ) {
            Column(
                modifier = Modifier.padding(GemTheme.spacing.panelPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.panelPadding),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(GemTheme.spacing.operationSpinnerSize),
                    strokeWidth = GemTheme.spacing.borderWidth,
                    color = GemTheme.colors.secondary,
                )
                Text(
                    text = message,
                    style = GemTheme.typeScale.body,
                    color = GemTheme.colors.secondary,
                )
            }
        }
    }
}
