package org.gem.apps.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.gem.ui.GemApp
import org.gem.ui.runtime.GemStartupLanguageSelection

class GemAndroidActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = GemAndroidCompositionRoot.create(applicationContext)
        val initialTextSelection = GemStartupLanguageSelection.initial(runtime)
        setContent {
            GemApp(
                runtime = runtime,
                initialTextSelection = initialTextSelection,
                onExitReady = { finish() },
            )
        }
    }
}
