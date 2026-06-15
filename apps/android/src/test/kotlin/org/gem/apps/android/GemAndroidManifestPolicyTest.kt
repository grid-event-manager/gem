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

    @Test
    fun `gem application disables native action bar`() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        val styles = File("src/main/res/values/styles.xml").readText()

        assertTrue(manifest.contains("""android:theme="@style/GemAndroidTheme""""))
        assertTrue(styles.contains("""parent="@android:style/Theme.Material.Light.NoActionBar""""))
        assertTrue(styles.contains("""<item name="android:windowActionBar">false</item>"""))
        assertTrue(styles.contains("""<item name="android:windowNoTitle">true</item>"""))
    }

    @Test
    fun `gem activity stays lifecycle shell for back and exit`() {
        val activity = File("src/main/kotlin/org/gem/apps/android/GemAndroidActivity.kt").readText()
        val forbiddenRouteTerms = listOf(
            "UiRoute",
            "GemAppController",
            "runLogoutWorkflow",
            "backFromSection",
            "BackHandler",
            "onBackPressed",
        )

        assertTrue(activity.contains("onExitReady = { finish() }"))
        forbiddenRouteTerms.forEach { term ->
            assertTrue(!activity.contains(term), "Activity must not contain $term")
        }
    }
}
