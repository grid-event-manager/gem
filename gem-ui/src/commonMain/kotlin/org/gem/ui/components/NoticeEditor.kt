package org.gem.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.design.GemTheme
import org.gem.ui.state.NoticeComposerUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun NoticeEditor(
    state: NoticeComposerUiState,
    textCatalogue: GemTextCatalogue,
    onSubjectChanged: (String) -> Unit,
    onBodyChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GemPanel(modifier = modifier) {
        GemTextField(
            label = textCatalogue.text(GemTextKey.Subject),
            value = state.subject,
            onValueChange = onSubjectChanged,
            modifier = Modifier.testTag(GemTestTags.Subject),
        )
        GemTextField(
            label = textCatalogue.text(GemTextKey.Body),
            value = state.body,
            onValueChange = onBodyChanged,
            singleLine = false,
            minHeight = GemTheme.spacing.noticeBodyMinHeight,
            maxHeight = GemTheme.spacing.noticeBodyMaxHeight,
            modifier = Modifier.testTag(GemTestTags.Body),
        )
        Text(
            text = textCatalogue.text(GemTextKey.DraftCharCount(state.charCount)),
            style = GemTheme.typeScale.smallLabel,
            color = GemTheme.colors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(GemTestTags.DraftCount),
        )
    }
}
