package org.hostess.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.NoticeComposerUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun NoticeEditor(
    state: NoticeComposerUiState,
    textCatalogue: HostessTextCatalogue,
    onSubjectChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        HostessTextField(
            label = textCatalogue.text(HostessTextKey.Subject),
            value = state.subject,
            onValueChange = onSubjectChanged,
            modifier = Modifier.testTag(HostessTestTags.Subject),
        )
        HostessTextField(
            label = textCatalogue.text(HostessTextKey.Body),
            value = state.body,
            onValueChange = onBodyChanged,
            singleLine = false,
            minHeight = HostessTheme.spacing.noticeBodyMinHeight,
            maxHeight = HostessTheme.spacing.noticeBodyMaxHeight,
            modifier = Modifier.testTag(HostessTestTags.Body),
        )
        Text(
            text = textCatalogue.text(HostessTextKey.DraftCharCount(state.charCount)),
            style = HostessTheme.typeScale.smallLabel,
            color = HostessTheme.colors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HostessTestTags.DraftCount),
        )
    }
}
