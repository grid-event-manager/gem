package org.gem.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.gem.ui.design.GemTheme
import org.gem.ui.state.SendFailureDetailUiState
import org.gem.ui.state.SendFooterUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun GemSendFooter(
    state: SendFooterUiState,
    textCatalogue: GemTextCatalogue,
    onPrimaryAction: () -> Unit,
    onFailureDetailsToggle: () -> Unit,
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
                            modifier = Modifier.size(GemTheme.spacing.operationSpinnerSize),
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
                    val hasFailureDetails = state.failureDetails.isNotEmpty()
                    Text(
                        text = textCatalogue.text(statusTextKey),
                        style = GemTheme.typeScale.smallLabel,
                        color = GemTheme.colors.secondary,
                        modifier = Modifier
                            .testTag(GemTestTags.StatusText)
                            .then(
                                if (hasFailureDetails) {
                                    Modifier.clickable(role = Role.Button, onClick = onFailureDetailsToggle)
                                } else {
                                    Modifier
                                },
                            ),
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
        AnimatedVisibility(
            visible = state.failureDetailsExpanded && state.failureDetails.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SendFailureDetailsPane(
                detailsText = state.failureDetails.failureDetailsText(textCatalogue),
                scrollable = state.failureDetails.size > SendFailureDetailsMaxVisibleRows,
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

@Composable
private fun SendFailureDetailsPane(
    detailsText: String,
    scrollable: Boolean,
) {
    if (scrollable) {
        GemScrollablePane(
            minHeight = GemTheme.spacing.compactRowMinHeight,
            maxHeight = GemTheme.spacing.sendFailureDetailsMaxHeight,
            testTag = GemTestTags.StatusText,
        ) {
            SendFailureDetailsText(detailsText)
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GemTheme.colors.fieldSurface,
        contentColor = GemTheme.colors.ink,
        shape = GemTheme.shapes.control,
        border = BorderStroke(GemTheme.spacing.borderWidth, GemTheme.colors.lineStrong),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GemTheme.spacing.compactRowMinHeight)
                .padding(GemTheme.spacing.fieldGap)
                .testTag(GemTestTags.StatusText),
        ) {
            SendFailureDetailsText(detailsText)
        }
    }
}

@Composable
private fun SendFailureDetailsText(detailsText: String) {
    Text(
        text = detailsText,
        style = GemTheme.typeScale.smallLabel,
        color = GemTheme.colors.danger,
    )
}

private fun List<SendFailureDetailUiState>.failureDetailsText(
    textCatalogue: GemTextCatalogue,
): String =
    joinToString(separator = "\n") { detail ->
        textCatalogue.text(
            GemTextKey.SendFailureDetailLine(
                groupName = detail.groupName,
                reason = textCatalogue.text(detail.reasonKey),
            ),
        )
    }

private const val SendFailureDetailsMaxVisibleRows = 10
