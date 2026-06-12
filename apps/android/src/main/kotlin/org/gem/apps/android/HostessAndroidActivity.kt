package org.gem.apps.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.gem.ui.GemApp

class HostessAndroidActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val runtime = HostessAndroidCompositionRoot.create(applicationContext)
        setContent {
            GemApp(runtime)
        }
    }
}
