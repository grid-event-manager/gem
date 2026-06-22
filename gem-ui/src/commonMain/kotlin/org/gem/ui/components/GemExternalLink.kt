package org.gem.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import org.gem.ui.design.GemColors
import org.gem.ui.design.GemTheme
import org.gem.ui.design.GemTypeScale

@Composable
fun GemExternalLink(
    text: String,
    uri: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = text,
        modifier = modifier
            .clickable(role = Role.Button) {
                uriHandler.openUri(uri)
            }
            .pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true),
        style = GemExternalLinkTokens.textStyle(GemTheme.typeScale),
        color = GemExternalLinkTokens.textColor(GemTheme.colors),
    )
}

internal object GemExternalLinkTokens {
    fun textStyle(typeScale: GemTypeScale): TextStyle =
        typeScale.button.copy(textDecoration = TextDecoration.Underline)

    fun textColor(colors: GemColors) =
        colors.buttonLabelInk
}
