package org.hostess.ui.components

import androidx.compose.runtime.Composable
import org.hostess.core.domain.InventoryItemKind
import org.hostess.ui.state.InventoryAssetRowUiState
import org.hostess.ui.state.InventoryFolderRowUiState
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.text.HostessTextCatalogue

@Composable
fun InventoryFolderRow(
    row: InventoryFolderRowUiState,
    textCatalogue: HostessTextCatalogue,
    onOpen: () -> Unit,
) {
    HostessSelectableRow(
        title = row.displayName,
        selected = false,
        onClick = onOpen,
        compact = true,
        titleStyle = HostessTheme.typeScale.smallLabel,
    )
}

@Composable
fun InventoryAssetRow(
    row: InventoryAssetRowUiState,
    textCatalogue: HostessTextCatalogue,
    onSelect: () -> Unit,
) {
    HostessSelectableRow(
        title = row.displayName,
        selected = row.selected,
        onClick = onSelect,
        compact = true,
        titleStyle = HostessTheme.typeScale.smallLabel,
        leading = if (row.kind == InventoryItemKind.LANDMARK) {
            { HostessLandmarkIcon() }
        } else {
            null
        },
    )
}
