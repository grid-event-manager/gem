package org.gem.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.gem.ui.components.GemExternalLink
import org.gem.ui.components.GemPanel
import org.gem.ui.design.GemTheme
import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.GemTextCatalogue
import org.gem.ui.text.GemTextKey

@Composable
fun AboutScreen(
    textCatalogue: GemTextCatalogue,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(GemTestTags.ViewAbout),
        verticalArrangement = Arrangement.spacedBy(GemTheme.spacing.rowGap),
    ) {
        GemPanel {
            AboutScreenInteraction.contentKeys.forEach { key ->
                Text(
                    text = textCatalogue.text(key),
                    style = if (key == GemTextKey.AboutProductLine) {
                        GemTheme.typeScale.sectionTitle
                    } else {
                        GemTheme.typeScale.body
                    },
                    color = GemTheme.colors.ink,
                )
            }
            GemExternalLink(
                text = textCatalogue.text(AboutScreenInteraction.helpLinkKey),
                uri = AboutScreenInteraction.helpSupportUri,
                modifier = Modifier.testTag(GemTestTags.AboutHelpSupport),
            )
        }
    }
}

internal object AboutScreenInteraction {
    const val helpSupportUri: String = "https://gem.anvll.com"
    val contentKeys: List<GemTextKey> = listOf(
        GemTextKey.AboutProductLine,
        GemTextKey.AboutCopyright,
        GemTextKey.AboutLicense,
    )
    val helpLinkKey: GemTextKey = GemTextKey.AboutHelpSupport
}
