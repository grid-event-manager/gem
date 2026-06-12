package org.gem.apps.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.gem.ui.GemApp
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey

fun main() {
    GemDesktopSingleInstanceGuard.terminateOtherInstances()
    val runtime = GemDesktopCompositionRoot.create()
    application {
        var exitRequestSerial by remember { mutableStateOf(0) }
        Window(
            onCloseRequest = { exitRequestSerial += 1 },
            state = rememberWindowState(
                width = GemDesktopWindowMetrics.initialWidth,
                height = GemDesktopWindowMetrics.initialHeight,
            ),
            title = EnglishGemTextCatalogue.text(GemTextKey.AppName),
        ) {
            GemApp(
                runtime = runtime,
                exitRequestSerial = exitRequestSerial,
                onExitReady = ::exitApplication,
            )
        }
    }
}
