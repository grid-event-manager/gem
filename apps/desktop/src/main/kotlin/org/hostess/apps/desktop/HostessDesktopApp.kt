package org.hostess.apps.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.hostess.ui.HostessApp
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

fun main() {
    val runtime = HostessDesktopCompositionRoot.create()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = EnglishHostessTextCatalogue.text(HostessTextKey.AppName),
        ) {
            HostessApp(runtime)
        }
    }
}
