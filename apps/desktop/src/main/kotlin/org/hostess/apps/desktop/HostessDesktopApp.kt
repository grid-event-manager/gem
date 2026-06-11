package org.hostess.apps.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        var exitRequestSerial by remember { mutableStateOf(0) }
        Window(
            onCloseRequest = { exitRequestSerial += 1 },
            state = rememberWindowState(
                width = HostessDesktopWindowMetrics.initialWidth,
                height = HostessDesktopWindowMetrics.initialHeight,
            ),
            title = EnglishHostessTextCatalogue.text(HostessTextKey.AppName),
        ) {
            HostessApp(
                runtime = runtime,
                exitRequestSerial = exitRequestSerial,
                onExitReady = ::exitApplication,
            )
        }
    }
}
