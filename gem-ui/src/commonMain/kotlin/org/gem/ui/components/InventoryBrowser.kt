package org.gem.ui.components

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
import org.gem.core.domain.InventoryFolderId
import org.gem.core.domain.InventoryItemId
import org.gem.core.domain.InventoryItemKind
import org.gem.ui.design.GemTheme
import org.gem.ui.state.InventoryBrowserUiState
import org.gem.ui.state.InventoryShortcut
import org.gem.ui.state.SelectedAttachmentUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun InventoryBrowser(
    state: InventoryBrowserUiState,
    textCatalogue: GemTextCatalogue,
    onShortcutSelected: (InventoryShortcut) -> Unit,
    onFolderOpen: (InventoryFolderId) -> Unit,
    onAssetSelected: (InventoryItemId) -> Unit,
    onAttachmentCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GemPanel(modifier = modifier) {
        InventoryShortcutBar(
            state = state.shortcuts,
            textCatalogue = textCatalogue,
            onShortcutSelected = onShortcutSelected,
        )
        AttachmentStatusLine(
            selectedAttachment = state.selectedAttachment,
            textCatalogue = textCatalogue,
            onAttachmentCleared = onAttachmentCleared,
            modifier = Modifier.testTag(GemTestTags.AttachmentSummary),
        )
        GemScrollablePane(
            minHeight = GemTheme.spacing.inventoryPaneMinHeight,
            maxHeight = GemTheme.spacing.inventoryPaneMaxHeight,
            testTag = GemTestTags.InventoryList,
            contentPadding = PaddingValues(GemTheme.spacing.fieldGap),
            verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.borderWidth),
        ) {
            val hasRows = state.visibleFolderRows.isNotEmpty() || state.visibleAssetRows.isNotEmpty()
            when {
                state.loading -> LoadingInventoryPaneStatus(textCatalogue.text(GemTextKey.LoadingInventory))
                state.errorKey != null && !hasRows -> InventoryPaneStatus(textCatalogue.text(state.errorKey))
                !hasRows -> InventoryPaneStatus(textCatalogue.text(GemTextKey.InventoryEmpty))
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
    textCatalogue: GemTextCatalogue,
    onAttachmentCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(GemTheme.spacing.compactRowMinHeight),
        color = if (selectedAttachment == null) {
            GemTheme.colors.fieldSurface
        } else {
            GemTheme.colors.selectedBackground
        },
        contentColor = GemTheme.colors.secondary,
        shape = GemTheme.shapes.control,
        border = BorderStroke(GemTheme.spacing.borderWidth, GemTheme.colors.lineStrong),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(GemTheme.spacing.compactRowMinHeight)
                .padding(horizontal = GemTheme.spacing.rowHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (selectedAttachment?.kind) {
                InventoryItemKind.LANDMARK -> HostessLandmarkIcon()
                InventoryItemKind.TEXTURE -> GemTextureIcon()
                else -> Unit
            }
            Text(
                text = selectedAttachment?.displayName
                    ?: textCatalogue.text(GemTextKey.NoAttachmentsAdded),
                style = GemTheme.typeScale.smallLabel,
                color = if (selectedAttachment == null) {
                    GemTheme.colors.muted
                } else {
                    GemTheme.colors.secondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selectedAttachment != null) {
                HostessInlineIconButton(
                    onClick = onAttachmentCleared,
                    contentDescription = textCatalogue.text(GemTextKey.ClearAttachment),
                    modifier = Modifier.testTag(GemTestTags.ClearAttachment),
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
            .heightIn(min = GemTheme.spacing.inventoryPaneMinHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.padding(GemTheme.spacing.fieldGap),
            strokeWidth = GemTheme.spacing.borderWidth,
            color = GemTheme.colors.primary,
        )
        Text(
            text = text,
            style = GemTheme.typeScale.body,
            color = GemTheme.colors.muted,
        )
    }
}

@Composable
private fun InventoryPaneStatus(text: String) {
    Text(
        text = text,
        style = GemTheme.typeScale.body,
        color = GemTheme.colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GemTheme.spacing.rowHorizontalPadding),
    )
}
