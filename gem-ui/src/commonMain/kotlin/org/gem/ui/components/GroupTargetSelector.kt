package org.gem.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.design.GemTheme
import org.gem.ui.state.GroupTargetUiState
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun GroupTargetSelector(
    state: GroupTargetUiState,
    textCatalogue: GemTextCatalogue,
    onAllGroupsChanged: (Boolean) -> Unit,
    onManualGroupsChanged: (Boolean) -> Unit,
    onManualGroupSelected: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    GemPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = textCatalogue.text(GemTextKey.Groups),
                style = GemTheme.typeScale.sectionTitle,
                color = GemTheme.colors.ink,
            )
            Text(
                text = textCatalogue.text(GemTextKey.SelectedCount(state.selectedCount)),
                style = GemTheme.typeScale.smallLabel,
                color = GemTheme.colors.muted,
                modifier = Modifier.testTag(GemTestTags.GroupCount),
            )
        }
        GemCheckboxCard(
            text = textCatalogue.text(GemTextKey.AddAll),
            checked = state.mode == org.gem.ui.state.GroupTargetMode.ALL,
            onCheckedChange = onAllGroupsChanged,
            modifier = Modifier.testTag(GemTestTags.AllGroups),
        )
        GemCheckboxCard(
            text = textCatalogue.text(GemTextKey.AddGroups),
            checked = state.mode == org.gem.ui.state.GroupTargetMode.MANUAL,
            onCheckedChange = onManualGroupsChanged,
            modifier = Modifier.testTag(GemTestTags.ManualGroups),
        )
        if (state.pickerVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(GemTestTags.GroupPicker),
                verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
            ) {
                GemScrollablePane(
                    minHeight = GemTheme.spacing.controlHeight,
                    maxHeight = GemTheme.spacing.groupPickerMaxHeight,
                    testTag = GemTestTags.GroupList,
                    verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.fieldGap),
                ) {
                    if (state.errorKey != null) {
                        GroupPaneStatus(textCatalogue.text(state.errorKey))
                    }
                    when {
                        state.loading -> GroupPaneStatus(textCatalogue.text(GemTextKey.LoadingGroups))
                        state.rows.isEmpty() -> GroupPaneStatus(textCatalogue.text(GemTextKey.GroupsEmpty))
                        else -> {
                            state.rows.forEach { row ->
                                GroupRow(
                                    row = row,
                                    textCatalogue = textCatalogue,
                                    onSelectedChange = { selected ->
                                        onManualGroupSelected(row.displayName, selected)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupPaneStatus(text: String) {
    Text(
        text = text,
        style = GemTheme.typeScale.body,
        color = GemTheme.colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(GemTheme.spacing.rowHorizontalPadding),
    )
}
