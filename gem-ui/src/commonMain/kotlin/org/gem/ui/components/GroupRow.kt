package org.gem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.gem.ui.design.GemTheme
import org.gem.ui.state.GroupTargetRowUiState
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun GroupRow(
    row: GroupTargetRowUiState,
    textCatalogue: GemTextCatalogue,
    onSelectedChange: (Boolean) -> Unit,
) {
    HostessSelectableRow(
        title = row.displayName,
        subtitle = if (row.canSendNotices) {
            textCatalogue.text(GemTextKey.CanSendNotices)
        } else {
            null
        },
        selected = row.selected,
        enabled = row.canSendNotices,
        onClick = { onSelectedChange(!row.selected) },
        leading = {
            Checkbox(
                checked = row.selected,
                enabled = row.canSendNotices,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = GemTheme.colors.primary,
                    uncheckedColor = GemTheme.colors.lineStrong,
                    checkmarkColor = GemTheme.colors.primaryInk,
                    disabledCheckedColor = GemTheme.colors.disabledBackground,
                    disabledUncheckedColor = GemTheme.colors.line,
                ),
            )
            GroupSwatch(row.displayName)
        },
    )
}

@Composable
private fun GroupSwatch(displayName: String) {
    Box(
        modifier = Modifier
            .size(GemTheme.spacing.rowIconSize)
            .background(GemTheme.colors.primary, GemTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.take(1).uppercase(),
            color = GemTheme.colors.primaryInk,
            style = GemTheme.typeScale.button,
        )
    }
}
