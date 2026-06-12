package org.hostess.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.SendFooterUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue

@Composable
fun HostessSendFooter(
    state: SendFooterUiState,
    textCatalogue: HostessTextCatalogue,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) {
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HostessTestTags.SendBar),
        verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
    ) {
        state.statusTextKey?.let { statusTextKey ->
            if (textCatalogue.text(statusTextKey).isNotBlank()) {
                if (state.sending) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(HostessTestTags.StatusText),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(HostessTheme.spacing.statusPillMinHeight),
                            strokeWidth = HostessTheme.spacing.borderWidth,
                            color = HostessTheme.colors.primary,
                        )
                        Text(
                            text = textCatalogue.text(statusTextKey),
                            style = HostessTheme.typeScale.smallLabel,
                            color = HostessTheme.colors.secondary,
                        )
                    }
                } else {
                    Text(
                        text = textCatalogue.text(statusTextKey),
                        style = HostessTheme.typeScale.smallLabel,
                        color = HostessTheme.colors.secondary,
                        modifier = Modifier.testTag(HostessTestTags.StatusText),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = state.showMissingRequirements && !state.enabled && state.missingRequirementKeys.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(
                text = state.missingRequirementKeys.joinToString(separator = " | ") { textCatalogue.text(it) },
                style = HostessTheme.typeScale.smallLabel,
                color = HostessTheme.colors.danger,
                modifier = Modifier.testTag(HostessTestTags.StatusText),
            )
        }
        state.detailText?.takeIf(String::isNotBlank)?.let { detailText ->
            Text(
                text = detailText,
                style = HostessTheme.typeScale.smallLabel,
                color = HostessTheme.colors.danger,
                modifier = Modifier.testTag(HostessTestTags.StatusText),
            )
        }
        HostessPrimaryButton(
            text = textCatalogue.text(state.primaryLabelKey),
            onClick = onPrimaryAction,
            enabled = !state.sending,
            visuallyEnabled = state.enabled && !state.sending,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.PrimaryAction),
        )
    }
}
