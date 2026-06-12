package org.gem.apps.android

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test

class GemAndroidActivityInstrumentedTest {
    @Test
    fun launchesSharedGemAppActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val intent = Intent(context, GemAndroidActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as GemAndroidActivity

        try {
            instrumentation.waitForIdleSync()
            assertFalse(activity.isFinishing)
        } finally {
            activity.finish()
            instrumentation.waitForIdleSync()
        }
    }
}
