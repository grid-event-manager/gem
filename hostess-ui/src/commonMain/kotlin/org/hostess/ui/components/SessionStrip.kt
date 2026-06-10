package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
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
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.SessionStripUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue

@Composable
fun SessionStrip(
    state: SessionStripUiState,
    textCatalogue: HostessTextCatalogue,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) {
        return
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = HostessTheme.spacing.sessionStripMinHeight)
            .testTag(HostessTestTags.SessionStrip),
        color = HostessTheme.colors.surfaceStrong,
        contentColor = HostessTheme.colors.ink,
        shape = HostessTheme.shapes.panel,
    ) {
        Row(
            modifier = Modifier.padding(HostessTheme.spacing.panelPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.locationLabel,
                style = HostessTheme.typeScale.body,
                modifier = Modifier.testTag(HostessTestTags.SessionLocation),
            )
            Surface(
                shape = HostessTheme.shapes.pill,
                color = if (state.online) {
                    HostessTheme.colors.successBackground
                } else {
                    HostessTheme.colors.statusBackground
                },
            ) {
                Text(
                    text = textCatalogue.text(state.statusKey),
                    style = HostessTheme.typeScale.statusPill,
                    color = if (state.online) HostessTheme.colors.successInk else HostessTheme.colors.muted,
                    modifier = Modifier
                        .padding(horizontal = HostessTheme.spacing.statusPillHorizontalPadding)
                        .heightIn(min = HostessTheme.spacing.statusPillMinHeight)
                        .testTag(HostessTestTags.SessionStatus),
                )
            }
        }
    }
}
