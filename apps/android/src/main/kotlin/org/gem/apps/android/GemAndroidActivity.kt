package org.gem.apps.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.gem.ui.GemApp

class GemAndroidActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = GemAndroidCompositionRoot.create(applicationContext)
        setContent {
            GemApp(runtime, onExitReady = { finish() })
        }
    }
}
