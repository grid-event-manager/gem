package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.design.GemTheme
import org.gem.ui.state.SessionStripUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue

@Composable
fun SessionStrip(
    state: SessionStripUiState,
    textCatalogue: GemTextCatalogue,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) {
        return
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GemTheme.spacing.statusPillMinHeight)
            .testTag(GemTestTags.SessionStrip),
        color = GemTheme.colors.surfaceStrong,
        contentColor = GemTheme.colors.ink,
        shape = GemTheme.shapes.panel,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = GemTheme.spacing.panelPadding,
                vertical = GemTheme.spacing.fieldGap,
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.locationLabel,
                style = GemTheme.typeScale.smallLabel,
                modifier = Modifier.testTag(GemTestTags.SessionLocation),
            )
            Surface(
                shape = GemTheme.shapes.pill,
                color = if (state.online) {
                    GemTheme.colors.successBackground
                } else {
                    GemTheme.colors.statusBackground
                },
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = GemTheme.spacing.statusPillHorizontalPadding)
                        .heightIn(min = GemTheme.spacing.statusPillMinHeight)
                        .testTag(GemTestTags.SessionStatus),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = textCatalogue.text(state.statusKey),
                        style = GemTheme.typeScale.statusPill,
                        color = if (state.online) GemTheme.colors.successInk else GemTheme.colors.muted,
                    )
                }
            }
        }
    }
}
