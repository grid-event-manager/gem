package org.hostess.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
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
    HostessPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HostessTestTags.SendBar),
    ) {
        state.statusTextKey?.let { statusTextKey ->
            Text(
                text = textCatalogue.text(statusTextKey),
                style = HostessTheme.typeScale.smallLabel,
                color = HostessTheme.colors.muted,
                modifier = Modifier.testTag(HostessTestTags.StatusText),
            )
        }
        HostessPrimaryButton(
            text = textCatalogue.text(state.primaryLabelKey),
            onClick = onPrimaryAction,
            enabled = state.enabled && !state.sending,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.PrimaryAction),
        )
    }
}
