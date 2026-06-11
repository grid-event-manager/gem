package org.hostess.apps.android

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class HostessAndroidManifestPolicyTest {
    @Test
    fun `hostess activity uses single task launch mode`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:launchMode="singleTask""""))
    }
}
