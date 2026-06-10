package org.hostess.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.hostess.core.domain.InventoryItemKind
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.InventoryAssetRowUiState
import org.hostess.ui.state.InventoryFolderRowUiState
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun InventoryFolderRow(
    row: InventoryFolderRowUiState,
    textCatalogue: HostessTextCatalogue,
    onOpen: () -> Unit,
) {
    HostessSelectableRow(
        title = row.displayName,
        subtitle = textCatalogue.text(HostessTextKey.Folder),
        selected = false,
        onClick = onOpen,
        trailing = {
            InventoryRowAction(textCatalogue.text(HostessTextKey.Open))
        },
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
        subtitle = textCatalogue.text(row.kind.textKey()),
        selected = row.selected,
        onClick = onSelect,
        trailing = {
            InventoryRowAction(
                textCatalogue.text(
                    if (row.selected) {
                        HostessTextKey.Selected
                    } else {
                        HostessTextKey.Select
                    },
                ),
            )
        },
    )
}

private fun InventoryItemKind.textKey(): HostessTextKey =
    when (this) {
        InventoryItemKind.LANDMARK -> HostessTextKey.Landmark
        InventoryItemKind.TEXTURE -> HostessTextKey.Texture
        InventoryItemKind.NOTECARD -> HostessTextKey.BlankStatus
    }

@Composable
private fun InventoryRowAction(text: String) {
    Text(
        text = text,
        color = HostessTheme.colors.secondary,
        style = HostessTheme.typeScale.smallLabel,
    )
}
