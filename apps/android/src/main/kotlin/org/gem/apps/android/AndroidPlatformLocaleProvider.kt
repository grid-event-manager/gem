package org.gem.apps.android

import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import org.gem.ui.text.PlatformLocaleProvider

class AndroidPlatformLocaleProvider private constructor(
    private val localeTagProvider: () -> String,
) : PlatformLocaleProvider {
    constructor(context: Context) : this(
        {
            currentLocaleTag(
                configuration = context.resources.configuration,
                javaDefaultLocaleTag = { Locale.getDefault().toLanguageTag() },
            )
        },
    )

    override fun currentLocaleTag(): String =
        localeTagProvider()

    companion object {
        internal fun javaDefault(): AndroidPlatformLocaleProvider =
            AndroidPlatformLocaleProvider { Locale.getDefault().toLanguageTag() }

        internal fun currentLocaleTag(
            configuration: Configuration,
            javaDefaultLocaleTag: () -> String,
        ): String {
            val locales = configuration.locales
            val tags = (0 until locales.size()).map { index ->
                locales[index].toLanguageTag()
            }
            return currentLocaleTag(tags, javaDefaultLocaleTag)
        }

        internal fun currentLocaleTag(
            orderedLocaleTags: List<String>,
            javaDefaultLocaleTag: () -> String,
        ): String =
            orderedLocaleTags.firstOrNull() ?: javaDefaultLocaleTag()
    }
}
