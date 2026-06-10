package org.hostess.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
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
        InventoryShortcutBar(
            state = state.shortcuts,
            textCatalogue = textCatalogue,
            onShortcutSelected = onShortcutSelected,
        )
        if (state.currentPath.isNotEmpty()) {
            Text(
                text = state.currentPath.joinToString(PathSeparator),
                style = HostessTheme.typeScale.body,
                color = HostessTheme.colors.muted,
                modifier = Modifier.testTag(HostessTestTags.InventoryPath),
            )
        }
        HostessScrollablePane(
            minHeight = HostessTheme.spacing.inventoryPaneMinHeight,
            maxHeight = HostessTheme.spacing.inventoryPaneMaxHeight,
            testTag = HostessTestTags.InventoryList,
            contentPadding = PaddingValues(HostessTheme.spacing.fieldGap),
            verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.borderWidth),
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
        state.selectedAttachment?.let { attachment ->
            SelectedAttachmentChip(
                text = attachment.displayName,
                modifier = Modifier.testTag(HostessTestTags.AttachmentSummary),
            )
        } ?: Box(modifier = Modifier.testTag(HostessTestTags.AttachmentSummary))
    }
}

@Composable
private fun SelectedAttachmentChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = HostessTheme.colors.selectedBackground,
        contentColor = HostessTheme.colors.secondary,
        shape = HostessTheme.shapes.control,
        border = BorderStroke(HostessTheme.spacing.borderWidth, HostessTheme.colors.primary),
    ) {
        Text(
            text = text,
            style = HostessTheme.typeScale.body,
            color = HostessTheme.colors.secondary,
            modifier = Modifier.padding(
                horizontal = HostessTheme.spacing.rowHorizontalPadding,
                vertical = HostessTheme.spacing.menuItemVerticalPadding,
            ),
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
