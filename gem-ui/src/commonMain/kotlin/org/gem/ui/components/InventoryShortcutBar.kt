package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.design.GemTheme
import org.gem.ui.state.InventoryShortcut
import org.gem.ui.state.InventoryShortcutUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun InventoryShortcutBar(
    state: InventoryShortcutUiState,
    textCatalogue: GemTextCatalogue,
    onShortcutSelected: (InventoryShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GemTestTags.InventoryTarget),
        horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
    ) {
        HostessSegmentButton(
            text = textCatalogue.text(GemTextKey.Landmarks),
            selected = state.landmarksSelected,
            onClick = { onShortcutSelected(InventoryShortcut.LANDMARKS) },
            modifier = Modifier.weight(weight = 1f),
        )
        HostessSegmentButton(
            text = textCatalogue.text(GemTextKey.Inventory),
            selected = state.rootSelected,
            onClick = { onShortcutSelected(InventoryShortcut.ROOT) },
            modifier = Modifier.weight(weight = 1f),
        )
        HostessSegmentButton(
            text = textCatalogue.text(GemTextKey.Textures),
            selected = state.texturesSelected,
            onClick = { onShortcutSelected(InventoryShortcut.TEXTURES) },
            modifier = Modifier.weight(weight = 1f),
        )
    }
}
