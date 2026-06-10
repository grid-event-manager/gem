package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.InventoryShortcut
import org.hostess.ui.state.InventoryShortcutUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun InventoryShortcutBar(
    state: InventoryShortcutUiState,
    textCatalogue: HostessTextCatalogue,
    onShortcutSelected: (InventoryShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HostessTestTags.InventoryTarget),
        horizontalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
    ) {
        HostessSegmentButton(
            text = textCatalogue.text(HostessTextKey.Inventory),
            selected = state.rootSelected,
            onClick = { onShortcutSelected(InventoryShortcut.ROOT) },
            modifier = Modifier.weight(weight = 1f),
        )
        HostessSegmentButton(
            text = textCatalogue.text(HostessTextKey.Landmarks),
            selected = state.landmarksSelected,
            onClick = { onShortcutSelected(InventoryShortcut.LANDMARKS) },
            modifier = Modifier.weight(weight = 1f),
        )
        HostessSegmentButton(
            text = textCatalogue.text(HostessTextKey.Textures),
            selected = state.texturesSelected,
            onClick = { onShortcutSelected(InventoryShortcut.TEXTURES) },
            modifier = Modifier.weight(weight = 1f),
        )
    }
}
