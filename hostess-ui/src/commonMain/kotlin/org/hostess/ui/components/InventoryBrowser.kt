package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemId
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun InventoryBrowser(
    state: InventoryBrowserUiState,
    textCatalogue: HostessTextCatalogue,
    onShortcutSelected: (InventoryShortcut) -> Unit,
    onFolderOpen: (InventoryFolderId) -> Unit,
    onAssetSelected: (InventoryItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        Text(
            text = textCatalogue.text(HostessTextKey.Inventory),
            style = HostessTheme.typeScale.sectionTitle,
            color = HostessTheme.colors.ink,
        )
        InventoryShortcutBar(
            state = state.shortcuts,
            textCatalogue = textCatalogue,
            onShortcutSelected = onShortcutSelected,
        )
        Text(
            text = textCatalogue.text(HostessTextKey.Folder),
            style = HostessTheme.typeScale.smallLabel,
            color = HostessTheme.colors.muted,
        )
        Text(
            text = state.currentPath.joinToString(PathSeparator),
            style = HostessTheme.typeScale.body,
            color = HostessTheme.colors.ink,
            modifier = Modifier.testTag(HostessTestTags.InventoryPath),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = HostessTheme.spacing.inventoryPaneMaxHeight)
                .verticalScroll(rememberScrollState())
                .testTag(HostessTestTags.InventoryList),
            verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
        ) {
            val hasRows = state.visibleFolderRows.isNotEmpty() || state.visibleAssetRows.isNotEmpty()
            when {
                state.loading -> InventoryPaneStatus(textCatalogue.text(HostessTextKey.LoadingInventory))
                state.errorKey != null && !hasRows -> InventoryPaneStatus(textCatalogue.text(state.errorKey))
                !hasRows -> InventoryPaneStatus(textCatalogue.text(HostessTextKey.InventoryEmpty))
                else -> {
                    state.visibleFolderRows.forEach { row ->
                        InventoryFolderRow(
                            row = row,
                            textCatalogue = textCatalogue,
                            onOpen = { onFolderOpen(row.folderId) },
                        )
                    }
                    state.visibleAssetRows.forEach { row ->
                        InventoryAssetRow(
                            row = row,
                            textCatalogue = textCatalogue,
                            onSelect = { onAssetSelected(row.itemId) },
                        )
                    }
                }
            }
        }
        Text(
            text = textCatalogue.text(state.attachmentSummary),
            style = HostessTheme.typeScale.smallLabel,
            color = HostessTheme.colors.muted,
            modifier = Modifier.testTag(HostessTestTags.AttachmentSummary),
        )
    }
}

@Composable
private fun InventoryPaneStatus(text: String) {
    Text(
        text = text,
        style = HostessTheme.typeScale.body,
        color = HostessTheme.colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(HostessTheme.spacing.rowHorizontalPadding),
    )
}

private const val PathSeparator = " / "
