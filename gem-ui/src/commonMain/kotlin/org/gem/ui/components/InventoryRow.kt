package org.gem.ui.components

import androidx.compose.runtime.Composable
import org.gem.core.domain.InventoryItemKind
import org.gem.ui.state.InventoryAssetRowUiState
import org.gem.ui.state.InventoryFolderRowUiState
import org.gem.ui.design.GemTheme
import org.gem.ui.text.GemTextCatalogue

@Composable
fun InventoryFolderRow(
    row: InventoryFolderRowUiState,
    textCatalogue: GemTextCatalogue,
    onOpen: () -> Unit,
) {
    HostessSelectableRow(
        title = row.displayName,
        selected = false,
        onClick = onOpen,
        compact = true,
        titleStyle = GemTheme.typeScale.smallLabel,
    )
}

@Composable
fun InventoryAssetRow(
    row: InventoryAssetRowUiState,
    textCatalogue: GemTextCatalogue,
    onSelect: () -> Unit,
) {
    HostessSelectableRow(
        title = row.displayName,
        selected = row.selected,
        onClick = onSelect,
        compact = true,
        titleStyle = GemTheme.typeScale.smallLabel,
        leading = if (row.kind == InventoryItemKind.LANDMARK) {
            { HostessLandmarkIcon() }
        } else {
            null
        },
    )
}
