package org.gem.preferences

import org.gem.core.appearance.AppearanceColor
import org.gem.core.appearance.AppearanceDraft
import org.gem.core.appearance.AppearanceElementTarget
import org.gem.core.appearance.AppearanceFontFamily
import org.gem.core.appearance.AppearanceMode
import org.gem.core.appearance.AppearanceProfile
import org.gem.core.appearance.AppearanceProfileCatalogue
import org.gem.core.appearance.AppearanceProfileId
import org.gem.core.appearance.AppearanceProfileName
import org.gem.core.appearance.AppearanceProfileSource
import org.gem.core.appearance.AppearanceProfileStoreLoadResult
import org.gem.core.appearance.AppearanceProfileStoreSnapshot
import org.gem.core.appearance.AppearanceTextTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AppearanceProfileFileCodecTest {
    private val codec = AppearanceProfileFileCodec()

    @Test
    fun `round trip preserves active ids profiles escaping and deterministic order`() {
        val dark = customProfile(
            id = "custom:dark:zeta",
            name = "Zeta",
            mode = AppearanceMode.DARK,
            bodyColor = "#123456",
        )
        val light = customProfile(
            id = "custom:light:a%3Dline",
            name = "A=Line\nTheme",
            mode = AppearanceMode.LIGHT,
            bodyColor = "#abcdef",
            font = "Serif=Display%",
        )
        val snapshot = AppearanceProfileStoreSnapshot(
            customProfiles = listOf(dark, light),
            activeLightProfileId = light.id,
            activeDarkProfileId = AppearanceProfileId("stock-goth-dark"),
        )

        val encoded = codec.encode(snapshot)
        val lines = encoded.lines()

        assertEquals("formatVersion=1", lines[0])
        assertEquals("activeProfile.light=custom:light:a%253Dline", lines[1])
        assertEquals("activeProfile.dark=stock-goth-dark", lines[2])
        assertTrue(encoded.indexOf("profile.0.id=custom:light") < encoded.indexOf("profile.1.id=custom:dark"))
        assertTrue("profile.0.name=A%3DLine%0ATheme" in lines)
        assertTrue("profile.0.text.body.font=Serif%3DDisplay%25" in lines)

        val loaded = assertIs<AppearanceProfileStoreLoadResult.Loaded>(codec.decode(encoded))

        assertEquals(snapshot.activeLightProfileId, loaded.snapshot.activeLightProfileId)
        assertEquals(snapshot.activeDarkProfileId, loaded.snapshot.activeDarkProfileId)
        assertEquals(listOf(light, dark), loaded.snapshot.customProfiles)
    }

    @Test
    fun `decode rejects invalid data clusters without partial profile set`() {
        val valid = codec.encode(
            AppearanceProfileStoreSnapshot(
                customProfiles = listOf(customProfile(id = "custom:light:one")),
                activeLightProfileId = null,
                activeDarkProfileId = null,
            ),
        )

        assertInvalid("invalid_format_version", valid.replace("formatVersion=1", "formatVersion=2"))
        assertInvalid("missing_active_profile", valid.withoutLine("activeProfile.light="))
        assertInvalid("malformed_percent_escape", valid.replace("Theme", "Theme%ZZ"))
        assertInvalid("invalid_target:unknown", valid.replace("profile.0.text.title.font", "profile.0.text.unknown.font"))
        assertInvalid("invalid_target:unknown", valid.replace("profile.0.element.page.color", "profile.0.element.unknown.color"))
        assertInvalid("invalid_mode", valid.replace("profile.0.mode=LIGHT", "profile.0.mode=BLUE"))
        assertInvalid("invalid_colour", valid.replace("profile.0.text.body.color=#444444", "profile.0.text.body.color=#ggg"))
        assertInvalid("blank_profile_id", valid.replace("profile.0.id=custom:light:one", "profile.0.id="))
        assertInvalid("blank_profile_name", valid.replace("profile.0.name=Theme", "profile.0.name="))
        assertInvalid("blank_font_family", valid.replace("profile.0.text.body.font=Inter", "profile.0.text.body.font="))
        assertInvalid("non_contiguous_profile_index", valid.replace("profile.0.", "profile.1."))
        assertInvalid("missing_key:profile.0.text.title.font", valid.withoutLine("profile.0.text.title.font="))
        assertInvalid("malformed_key:profile.0.source", valid + "profile.0.source=STOCK\n")
    }

    @Test
    fun `decode rejects duplicate profile ids`() {
        val encoded = codec.encode(
            AppearanceProfileStoreSnapshot(
                customProfiles = listOf(
                    customProfile(id = "custom:light:one", name = "One", mode = AppearanceMode.LIGHT),
                    customProfile(id = "custom:light:one", name = "Two", mode = AppearanceMode.LIGHT),
                ),
                activeLightProfileId = null,
                activeDarkProfileId = null,
            ),
        )

        assertInvalid("duplicate_profile_id", encoded)
    }

    @Test
    fun `decode completes older custom profiles missing only new element targets`() {
        val encoded = codec.encode(
            AppearanceProfileStoreSnapshot(
                customProfiles = listOf(customProfile(id = "custom:dark:old", mode = AppearanceMode.DARK)),
                activeLightProfileId = null,
                activeDarkProfileId = null,
            ),
        )
        val older = COMPATIBLE_NEW_ELEMENT_TARGETS.fold(encoded) { text, target ->
            text.withoutLine("profile.0.element.${target.storageKey}.color=")
        }

        val loaded = assertIs<AppearanceProfileStoreLoadResult.Loaded>(codec.decode(older))
        val profile = loaded.snapshot.customProfiles.single()

        assertEquals("#8AB4C4", profile.elementColors.getValue(AppearanceElementTarget.ACCENT_TEXT).value)
        assertEquals("#B5544D", profile.elementColors.getValue(AppearanceElementTarget.ERROR_TEXT).value)
        assertEquals("#8AB4C4", profile.elementColors.getValue(AppearanceElementTarget.STATUS_TEXT).value)
        assertEquals("#A0B0BC", profile.elementColors.getValue(AppearanceElementTarget.MENU_DISABLED_TEXT).value)
        assertEquals("#C0C8D0", profile.elementColors.getValue(AppearanceElementTarget.INTERACTIVE_HOVER_TEXT).value)
    }

    @Test
    fun `decode rejects profiles missing pre-existing element targets`() {
        val encoded = codec.encode(
            AppearanceProfileStoreSnapshot(
                customProfiles = listOf(customProfile(id = "custom:light:bad")),
                activeLightProfileId = null,
                activeDarkProfileId = null,
            ),
        )

        assertInvalid(
            "missing_key:profile.0.element.${AppearanceElementTarget.PAGE_BACKGROUND.storageKey}.color",
            encoded.withoutLine("profile.0.element.${AppearanceElementTarget.PAGE_BACKGROUND.storageKey}.color="),
        )
    }

    @Test
    fun `snapshot and encode reject stock profiles`() {
        assertFailsWith<IllegalArgumentException> {
            AppearanceProfileStoreSnapshot(
                customProfiles = listOf(AppearanceProfileCatalogue.stockProfiles().first()),
                activeLightProfileId = null,
                activeDarkProfileId = null,
            )
        }
    }

    private fun assertInvalid(
        expectedReason: String,
        text: String,
    ) {
        val invalid = assertIs<AppearanceProfileStoreLoadResult.Invalid>(codec.decode(text))
        assertEquals(expectedReason, invalid.reason)
    }

    private fun String.withoutLine(prefix: String): String =
        lines()
            .filterNot { it.startsWith(prefix) }
            .joinToString("\n")

    private fun customProfile(
        id: String = "custom:light:theme",
        name: String = "Theme",
        mode: AppearanceMode = AppearanceMode.LIGHT,
        bodyColor: String = "#444444",
        font: String = "Inter",
    ): AppearanceProfile {
        val draft = AppearanceDraft.fromProfile(
            AppearanceProfileCatalogue.stockProfiles().first { it.mode == mode },
        )
        return AppearanceProfile(
            id = AppearanceProfileId(id),
            name = AppearanceProfileName(name),
            mode = mode,
            source = AppearanceProfileSource.CUSTOM,
            textFonts = draft.textFonts + (AppearanceTextTarget.MAIN_BODY to AppearanceFontFamily(font)),
            textColors = draft.textColors + (AppearanceTextTarget.MAIN_BODY to AppearanceColor.require(bodyColor)),
            elementColors = draft.elementColors + (
                AppearanceElementTarget.PAGE_BACKGROUND to AppearanceColor.require(
                    if (mode == AppearanceMode.LIGHT) "#fefefe" else "#010101",
                )
            ),
        )
    }

    private companion object {
        val COMPATIBLE_NEW_ELEMENT_TARGETS: List<AppearanceElementTarget> = listOf(
            AppearanceElementTarget.ACCENT_TEXT,
            AppearanceElementTarget.ERROR_TEXT,
            AppearanceElementTarget.STATUS_TEXT,
            AppearanceElementTarget.MENU_DISABLED_TEXT,
            AppearanceElementTarget.INTERACTIVE_HOVER_TEXT,
        )
    }
}
