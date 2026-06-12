package org.hostess.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.state.GroupTargetUiState
import org.hostess.ui.testtags.HostessTestTags
import org.hostess.ui.text.HostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

@Composable
fun GroupTargetSelector(
    state: GroupTargetUiState,
    textCatalogue: HostessTextCatalogue,
    onAllGroupsChanged: (Boolean) -> Unit,
    onManualGroupsChanged: (Boolean) -> Unit,
    onManualGroupSelected: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    HostessPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = textCatalogue.text(HostessTextKey.Groups),
                style = HostessTheme.typeScale.sectionTitle,
                color = HostessTheme.colors.ink,
            )
            Text(
                text = textCatalogue.text(HostessTextKey.SelectedCount(state.selectedCount)),
                style = HostessTheme.typeScale.smallLabel,
                color = HostessTheme.colors.muted,
                modifier = Modifier.testTag(HostessTestTags.GroupCount),
            )
        }
        HostessCheckboxCard(
            text = textCatalogue.text(HostessTextKey.AddAll),
            checked = state.mode == org.hostess.ui.state.GroupTargetMode.ALL,
            onCheckedChange = onAllGroupsChanged,
            modifier = Modifier.testTag(HostessTestTags.AllGroups),
        )
        HostessCheckboxCard(
            text = textCatalogue.text(HostessTextKey.AddGroups),
            checked = state.mode == org.hostess.ui.state.GroupTargetMode.MANUAL,
            onCheckedChange = onManualGroupsChanged,
            modifier = Modifier.testTag(HostessTestTags.ManualGroups),
        )
        if (state.pickerVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HostessTestTags.GroupPicker),
                verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
            ) {
                HostessScrollablePane(
                    minHeight = HostessTheme.spacing.controlHeight,
                    maxHeight = HostessTheme.spacing.groupPickerMaxHeight,
                    testTag = HostessTestTags.GroupList,
                    verticalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
                ) {
                    if (state.errorKey != null) {
                        GroupPaneStatus(textCatalogue.text(state.errorKey))
                    }
                    when {
                        state.loading -> GroupPaneStatus(textCatalogue.text(HostessTextKey.LoadingGroups))
                        state.rows.isEmpty() -> GroupPaneStatus(textCatalogue.text(HostessTextKey.GroupsEmpty))
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
        style = HostessTheme.typeScale.body,
        color = HostessTheme.colors.muted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(HostessTheme.spacing.rowHorizontalPadding),
    )
}
