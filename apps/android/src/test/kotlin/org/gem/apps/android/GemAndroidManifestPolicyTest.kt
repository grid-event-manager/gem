package org.gem.apps.android

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class GemAndroidManifestPolicyTest {
    @Test
    fun `gem activity uses single task launch mode`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains("""android:launchMode="singleTask""""))
    }
}
