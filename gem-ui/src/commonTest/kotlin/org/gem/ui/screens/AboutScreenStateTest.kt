package org.gem.ui.screens

import org.gem.ui.testtags.GemTestTags
import org.gem.ui.text.EnglishGemTextCatalogue
import org.gem.ui.text.GemTextKey
import kotlin.test.Test
import kotlin.test.assertEquals

class AboutScreenStateTest {
    @Test
    fun aboutScreenUsesCatalogueCopyAndSupportUri() {
        val catalogue = EnglishGemTextCatalogue

        assertEquals("data-view-about", GemTestTags.ViewAbout)
        assertEquals("data-about-help-support", GemTestTags.AboutHelpSupport)
        assertEquals(
            listOf(
                GemTextKey.AboutProductLine,
                GemTextKey.AboutCopyright,
                GemTextKey.AboutLicense,
            ),
            AboutScreenInteraction.contentKeys,
        )
        assertEquals(GemTextKey.AboutHelpSupport, AboutScreenInteraction.helpLinkKey)
        assertEquals("https://gem.anvll.com", AboutScreenInteraction.helpSupportUri)
        assertEquals("GEM - Grid Event Manager", catalogue.text(GemTextKey.AboutProductLine))
        assertEquals("Copyright (c) 2026 ANVLL", catalogue.text(GemTextKey.AboutCopyright))
        assertEquals("Released under the Apache License 2.0.", catalogue.text(GemTextKey.AboutLicense))
        assertEquals("Help & Support", catalogue.text(GemTextKey.AboutHelpSupport))
    }
}
