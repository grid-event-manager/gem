package org.hostess.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import org.hostess.core.domain.InventoryFolderId
import org.hostess.core.domain.InventoryItemId
import org.hostess.core.domain.InventoryItemKind
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.InventoryBrowserUiState
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.state.SelectedAttachmentUiState
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
    onAttachmentCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        InventoryShortcutBar(
            state = state.shortcuts,
            textCatalogue = textCatalogue,
            onShortcutSelected = onShortcutSelected,
        )
        AttachmentStatusLine(
            selectedAttachment = state.selectedAttachment,
            textCatalogue = textCatalogue,
            onAttachmentCleared = onAttachmentCleared,
            modifier = Modifier.testTag(HostessTestTags.AttachmentSummary),
        )
        HostessScrollablePane(
            minHeight = HostessTheme.spacing.inventoryPaneMinHeight,
            maxHeight = HostessTheme.spacing.inventoryPaneMaxHeight,
            testTag = HostessTestTags.InventoryList,
            contentPadding = PaddingValues(HostessTheme.spacing.fieldGap),
            verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.borderWidth),
        ) {
            val hasRows = state.visibleFolderRows.isNotEmpty() || state.visibleAssetRows.isNotEmpty()
            when {
                state.loading -> LoadingInventoryPaneStatus(textCatalogue.text(HostessTextKey.LoadingInventory))
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
    }
}

@Composable
private fun AttachmentStatusLine(
    selectedAttachment: SelectedAttachmentUiState?,
    textCatalogue: HostessTextCatalogue,
    onAttachmentCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(HostessTheme.spacing.compactRowMinHeight),
        color = if (selectedAttachment == null) {
            HostessTheme.colors.fieldSurface
        } else {
            HostessTheme.colors.selectedBackground
        },
        contentColor = HostessTheme.colors.secondary,
        shape = HostessTheme.shapes.control,
        border = BorderStroke(HostessTheme.spacing.borderWidth, HostessTheme.colors.lineStrong),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HostessTheme.spacing.compactRowMinHeight)
                .padding(horizontal = HostessTheme.spacing.rowHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (selectedAttachment?.kind) {
                InventoryItemKind.LANDMARK -> HostessLandmarkIcon()
                InventoryItemKind.TEXTURE -> HostessTextureIcon()
                else -> Unit
            }
            Text(
                text = selectedAttachment?.displayName
                    ?: textCatalogue.text(HostessTextKey.NoAttachmentsAdded),
                style = HostessTheme.typeScale.smallLabel,
                color = if (selectedAttachment == null) {
                    HostessTheme.colors.muted
                } else {
                    HostessTheme.colors.secondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selectedAttachment != null) {
                HostessInlineIconButton(
                    onClick = onAttachmentCleared,
                    contentDescription = textCatalogue.text(HostessTextKey.ClearAttachment),
                    modifier = Modifier.testTag(HostessTestTags.ClearAttachment),
                ) {
                    HostessClearIcon()
                }
            }
        }
    }
}

@Composable
private fun LoadingInventoryPaneStatus(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = HostessTheme.spacing.inventoryPaneMinHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(HostessTheme.spacing.fieldGap),
            strokeWidth = HostessTheme.spacing.borderWidth,
            color = HostessTheme.colors.primary,
        )
        Text(
            text = text,
            style = HostessTheme.typeScale.body,
            color = HostessTheme.colors.muted,
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
