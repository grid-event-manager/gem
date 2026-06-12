package org.gem.ui.components

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
import org.gem.ui.design.GemTheme
import org.gem.ui.state.SendFooterUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue

@Composable
fun GemSendFooter(
    state: SendFooterUiState,
    textCatalogue: GemTextCatalogue,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) {
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GemTestTags.SendBar),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
    ) {
        state.statusTextKey?.let { statusTextKey ->
            if (textCatalogue.text(statusTextKey).isNotBlank()) {
                if (state.sending) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(GemTestTags.StatusText),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(GemTheme.spacing.statusPillMinHeight),
                            strokeWidth = GemTheme.spacing.borderWidth,
                            color = GemTheme.colors.primary,
                        )
                        Text(
                            text = textCatalogue.text(statusTextKey),
                            style = GemTheme.typeScale.smallLabel,
                            color = GemTheme.colors.secondary,
                        )
                    }
                } else {
                    Text(
                        text = textCatalogue.text(statusTextKey),
                        style = GemTheme.typeScale.smallLabel,
                        color = GemTheme.colors.secondary,
                        modifier = Modifier.testTag(GemTestTags.StatusText),
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
                style = GemTheme.typeScale.smallLabel,
                color = GemTheme.colors.danger,
                modifier = Modifier.testTag(GemTestTags.StatusText),
            )
        }
        state.detailText?.takeIf(String::isNotBlank)?.let { detailText ->
            Text(
                text = detailText,
                style = GemTheme.typeScale.smallLabel,
                color = GemTheme.colors.danger,
                modifier = Modifier.testTag(GemTestTags.StatusText),
            )
        }
        GemPrimaryButton(
            text = textCatalogue.text(state.primaryLabelKey),
            onClick = onPrimaryAction,
            enabled = !state.sending,
            visuallyEnabled = state.enabled && !state.sending,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GemTestTags.PrimaryAction),
        )
    }
}
