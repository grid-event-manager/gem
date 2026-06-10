package org.hostess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.GroupTargetRowUiState
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun GroupRow(
    row: GroupTargetRowUiState,
    textCatalogue: HostessTextCatalogue,
    onSelectedChange: (Boolean) -> Unit,
) {
    HostessSelectableRow(
        title = row.displayName,
        subtitle = if (row.canSendNotices) {
            textCatalogue.text(HostessTextKey.CanSendNotices)
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
                    checkedColor = HostessTheme.colors.primary,
                    uncheckedColor = HostessTheme.colors.lineStrong,
                    checkmarkColor = HostessTheme.colors.primaryInk,
                    disabledCheckedColor = HostessTheme.colors.disabledBackground,
                    disabledUncheckedColor = HostessTheme.colors.line,
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
            .size(HostessTheme.spacing.rowIconSize)
            .background(HostessTheme.colors.primary, HostessTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.take(1).uppercase(),
            color = HostessTheme.colors.primaryInk,
            style = HostessTheme.typeScale.button,
        )
    }
}
