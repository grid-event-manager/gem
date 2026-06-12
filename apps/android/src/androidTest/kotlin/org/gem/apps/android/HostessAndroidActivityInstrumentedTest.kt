package org.gem.apps.android

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test

class HostessAndroidActivityInstrumentedTest {
    @Test
    fun launchesSharedGemAppActivity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val intent = Intent(context, HostessAndroidActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = instrumentation.startActivitySync(intent) as HostessAndroidActivity

        try {
            instrumentation.waitForIdleSync()
            assertFalse(activity.isFinishing)
        } finally {
            activity.finish()
            instrumentation.waitForIdleSync()
        }
    }
}
