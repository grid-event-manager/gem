package org.hostess.apps.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.hostess.ui.HostessApp
import org.hostess.ui.text.EnglishHostessTextCatalogue
import org.hostess.ui.text.HostessTextKey

fun main() {
    HostessDesktopSingleInstanceGuard.terminateOtherInstances()
    val runtime = HostessDesktopCompositionRoot.create()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(
                width = HostessDesktopWindowMetrics.initialWidth,
                height = HostessDesktopWindowMetrics.initialHeight,
            ),
            title = EnglishHostessTextCatalogue.text(HostessTextKey.AppName),
        ) {
            HostessApp(runtime)
        }
    }
}
