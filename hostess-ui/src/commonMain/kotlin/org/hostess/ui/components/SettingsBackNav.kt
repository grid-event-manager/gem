package org.hostess.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import org.hostess.ui.design.HostessTheme
import org.hostess.ui.testtags.HostessTestTags

@Composable
fun SettingsBackNav(
    text: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onBack)
            .testTag(HostessTestTags.SettingsBack),
        horizontalArrangement = Arrangement.spacedBy(HostessTheme.spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HostessBackIcon()
        Text(
            text = text,
            color = HostessTheme.colors.ink,
            style = HostessTheme.typeScale.smallLabel,
        )
    }
}
