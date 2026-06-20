package org.gem.core.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppearanceProfileServiceTest {
    @Test
    fun `missing store data returns stock profiles and null selected drafts`() {
        val result = service(LoadStore()).loadState()
        val state = assertIs<AppearanceProfileLoadResult.Loaded>(result).state

        assertEquals(6, state.stockProfiles.size)
        assertEquals(emptyList(), state.customProfiles)
        assertNull(state.warning)
        assertNull(state.selectedDraftFor(AppearanceMode.LIGHT))
        assertNull(state.selectedDraftFor(AppearanceMode.DARK))
    }

    @Test
    fun `invalid store data returns default state with rejected custom profile warning`() {
        val result = service(
            LoadStore(
                loadResult = AppearanceProfileStoreLoadResult.Invalid("bad-format"),
            ),
        ).loadState()
        val state = assertIs<AppearanceProfileLoadResult.Loaded>(result).state

        assertEquals(emptyList(), state.customProfiles)
        assertEquals(
            AppearanceProfileWarning.CustomProfileStoreRejected("bad-format"),
            state.warning,
        )
    }

    @Test
    fun `storage failure returns typed failure and caller visible null selected state`() {
        val result = service(
            LoadStore(
                loadResult = AppearanceProfileStoreLoadResult.StorageFailed("[redacted-read]"),
            ),
        ).loadState()
        val failure = assertIs<AppearanceProfileLoadResult.StorageFailed>(result)

        assertEquals("[redacted-read]", failure.message)
        assertEquals(6, failure.state.stockProfiles.size)
        assertNull(failure.state.selectedDraftFor(AppearanceMode.LIGHT))
    }

    @Test
    fun `select profile persists active profile for selected mode`() {
        val store = LoadStore()
        val result = service(store).selectProfile(AppearanceProfileId("stock-cyber-dark"))
        val selected = assertIs<AppearanceProfileSelectionResult.Selected>(result)

        assertEquals("stock-cyber-dark", selected.profile.id.value)
        assertEquals("stock-cyber-dark", selected.state.activeDarkProfileId?.value)
        assertEquals("stock-cyber-dark", selected.state.selectedDraftFor(AppearanceMode.DARK)?.selectedProfileId?.value)
        assertNull(selected.state.selectedDraftFor(AppearanceMode.LIGHT))
        assertNull(selected.state.activeLightProfileId)
        assertEquals("stock-cyber-dark", store.savedSnapshots.single().activeDarkProfileId?.value)
    }

    @Test
    fun `invalid active profile ids produce null selected drafts`() {
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = emptyList(),
                    activeLightProfileId = AppearanceProfileId("missing-light"),
                    activeDarkProfileId = AppearanceProfileId("stock-cyber-light"),
                ),
            ),
        )

        val state = assertIs<AppearanceProfileLoadResult.Loaded>(service(store).loadState()).state

        assertNull(state.selectedDraftFor(AppearanceMode.LIGHT))
        assertNull(state.selectedDraftFor(AppearanceMode.DARK))
    }

    @Test
    fun `select missing profile returns missing result without saving`() {
        val store = LoadStore()
        val result = service(store).selectProfile(AppearanceProfileId("missing-profile"))

        assertEquals(
            AppearanceProfileSelectionResult.ProfileMissing(AppearanceProfileId("missing-profile")),
            result,
        )
        assertEquals(emptyList(), store.savedSnapshots)
    }

    @Test
    fun `save profile creates custom profile and sets active mode`() {
        val store = LoadStore()
        val draft = stockDraft(AppearanceMode.LIGHT)
        val result = service(store).saveProfile(
            name = AppearanceProfileName("  My Theme  "),
            mode = AppearanceMode.LIGHT,
            draft = draft,
        )
        val saved = assertIs<AppearanceProfileSaveResult.Saved>(result)

        assertEquals("My Theme", saved.profile.name.value)
        assertEquals("custom:light:my-theme", saved.profile.id.value)
        assertEquals(AppearanceProfileSource.CUSTOM, saved.profile.source)
        assertEquals("custom:light:my-theme", saved.state.activeLightProfileId?.value)
        assertEquals(1, store.savedSnapshots.single().customProfiles.size)
    }

    @Test
    fun `save profile replaces same custom name and mode while preserving id`() {
        val existing = customProfile(
            id = "custom:dark:venue",
            name = "Venue",
            mode = AppearanceMode.DARK,
        )
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = listOf(existing),
                    activeLightProfileId = null,
                    activeDarkProfileId = null,
                ),
            ),
        )
        val draft = stockDraft(AppearanceMode.DARK).copy(
            textColors = stockDraft(AppearanceMode.DARK).textColors +
                (AppearanceTextTarget.MAIN_BODY to AppearanceColor.require("#123456")),
        )
        val result = service(store).saveProfile(
            name = AppearanceProfileName("venue"),
            mode = AppearanceMode.DARK,
            draft = draft,
        )
        val saved = assertIs<AppearanceProfileSaveResult.Saved>(result)

        assertEquals("custom:dark:venue", saved.profile.id.value)
        assertEquals("#123456", saved.profile.textColors.getValue(AppearanceTextTarget.MAIN_BODY).value)
        assertEquals(1, store.savedSnapshots.single().customProfiles.size)
    }

    @Test
    fun `save profile rejects mismatched draft mode and never saves`() {
        val store = LoadStore()
        val result = service(store).saveProfile(
            name = AppearanceProfileName("Wrong Mode"),
            mode = AppearanceMode.LIGHT,
            draft = stockDraft(AppearanceMode.DARK),
        )

        assertIs<AppearanceProfileSaveResult.Rejected>(result)
        assertEquals(emptyList(), store.savedSnapshots)
    }

    @Test
    fun `reset mode clears active profile without deleting custom profiles`() {
        val existing = customProfile(
            id = "custom:light:venue",
            name = "Venue",
            mode = AppearanceMode.LIGHT,
        )
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = listOf(existing),
                    activeLightProfileId = existing.id,
                    activeDarkProfileId = AppearanceProfileId("stock-goth-dark"),
                ),
            ),
        )
        val result = service(store).resetMode(AppearanceMode.LIGHT)
        val reset = assertIs<AppearanceProfileResetResult.Reset>(result)

        assertNull(reset.state.activeLightProfileId)
        assertEquals("stock-goth-dark", reset.state.activeDarkProfileId?.value)
        assertNull(reset.state.selectedDraftFor(AppearanceMode.LIGHT))
        assertEquals(listOf(existing), reset.state.customProfiles)
        assertEquals(listOf(existing), store.savedSnapshots.single().customProfiles)
    }

    @Test
    fun `reset all modes clears both active profiles without deleting custom profiles`() {
        val existing = customProfile(
            id = "custom:light:venue",
            name = "Venue",
            mode = AppearanceMode.LIGHT,
        )
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = listOf(existing),
                    activeLightProfileId = existing.id,
                    activeDarkProfileId = AppearanceProfileId("stock-goth-dark"),
                ),
            ),
        )
        val result = service(store).resetAllModes()
        val reset = assertIs<AppearanceProfileResetResult.Reset>(result)

        assertNull(reset.state.activeLightProfileId)
        assertNull(reset.state.activeDarkProfileId)
        assertNull(reset.state.selectedDraftFor(AppearanceMode.LIGHT))
        assertNull(reset.state.selectedDraftFor(AppearanceMode.DARK))
        assertEquals(listOf(existing), reset.state.customProfiles)
        assertEquals(listOf(existing), store.savedSnapshots.single().customProfiles)
    }

    @Test
    fun `mode switch maps stock profile to matching target mode family`() {
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = emptyList(),
                    activeLightProfileId = null,
                    activeDarkProfileId = AppearanceProfileId("stock-goth-dark"),
                ),
            ),
        )

        val result = service(store).switchModePreservingProfileFamily(
            targetMode = AppearanceMode.LIGHT,
            sourceProfileId = AppearanceProfileId("stock-goth-dark"),
        )
        val switched = assertIs<AppearanceProfileModeSwitchResult.Switched>(result)

        assertEquals("stock-goth-light", switched.state.activeLightProfileId?.value)
        assertEquals("stock-goth-dark", switched.state.activeDarkProfileId?.value)
        assertEquals("stock-goth-light", switched.state.selectedDraftFor(AppearanceMode.LIGHT)?.selectedProfileId?.value)
        assertEquals("stock-goth-light", store.savedSnapshots.single().activeLightProfileId?.value)
    }

    @Test
    fun `mode switch treats missing source as null source and leaves target active profile unchanged`() {
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = emptyList(),
                    activeLightProfileId = AppearanceProfileId("stock-cyber-light"),
                    activeDarkProfileId = null,
                ),
            ),
        )

        val result = service(store).switchModePreservingProfileFamily(
            targetMode = AppearanceMode.LIGHT,
            sourceProfileId = AppearanceProfileId("missing-profile"),
        )
        val switched = assertIs<AppearanceProfileModeSwitchResult.Switched>(result)

        assertEquals("stock-cyber-light", switched.state.activeLightProfileId?.value)
        assertEquals(emptyList(), store.savedSnapshots)
    }

    @Test
    fun `mode switch chooses smallest matching custom counterpart id`() {
        val source = customProfile(
            id = "custom:dark:venue",
            name = "Venue",
            mode = AppearanceMode.DARK,
        )
        val targetB = customProfile(
            id = "custom:light:venue-b",
            name = "venue",
            mode = AppearanceMode.LIGHT,
        )
        val targetA = customProfile(
            id = "custom:light:venue-a",
            name = "VENUE",
            mode = AppearanceMode.LIGHT,
        )
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = listOf(source, targetB, targetA),
                    activeLightProfileId = null,
                    activeDarkProfileId = source.id,
                ),
            ),
        )

        val result = service(store).switchModePreservingProfileFamily(
            targetMode = AppearanceMode.LIGHT,
            sourceProfileId = source.id,
        )
        val switched = assertIs<AppearanceProfileModeSwitchResult.Switched>(result)

        assertEquals("custom:light:venue-a", switched.state.activeLightProfileId?.value)
        assertEquals("custom:light:venue-a", store.savedSnapshots.single().activeLightProfileId?.value)
    }

    @Test
    fun `mode switch clears only target mode when counterpart is absent`() {
        val source = customProfile(
            id = "custom:dark:venue",
            name = "Venue",
            mode = AppearanceMode.DARK,
        )
        val store = LoadStore(
            loadResult = AppearanceProfileStoreLoadResult.Loaded(
                AppearanceProfileStoreSnapshot(
                    customProfiles = listOf(source),
                    activeLightProfileId = AppearanceProfileId("stock-princess-light"),
                    activeDarkProfileId = source.id,
                ),
            ),
        )

        val result = service(store).switchModePreservingProfileFamily(
            targetMode = AppearanceMode.LIGHT,
            sourceProfileId = source.id,
        )
        val switched = assertIs<AppearanceProfileModeSwitchResult.Switched>(result)

        assertNull(switched.state.activeLightProfileId)
        assertEquals(source.id, switched.state.activeDarkProfileId)
        assertNull(store.savedSnapshots.single().activeLightProfileId)
    }

    @Test
    fun `save and reset return storage failures without pretending to save`() {
        val store = LoadStore(saveResult = AppearanceProfileStoreSaveResult.StorageFailed("[redacted-write]"))
        val draft = stockDraft(AppearanceMode.LIGHT)

        assertEquals(
            AppearanceProfileSaveResult.StorageFailed("[redacted-write]"),
            service(store).saveProfile(AppearanceProfileName("Theme"), AppearanceMode.LIGHT, draft),
        )
        assertEquals(
            AppearanceProfileResetResult.StorageFailed("[redacted-write]"),
            service(store).resetMode(AppearanceMode.LIGHT),
        )
        assertEquals(
            AppearanceProfileModeSwitchResult.StorageFailed("[redacted-write]"),
            service(store).switchModePreservingProfileFamily(
                targetMode = AppearanceMode.LIGHT,
                sourceProfileId = AppearanceProfileId("stock-princess-dark"),
            ),
        )
    }

    private fun service(store: LoadStore): AppearanceProfileService =
        AppearanceProfileService(store)

    private fun stockDraft(mode: AppearanceMode): AppearanceDraft =
        AppearanceDraft.fromProfile(AppearanceProfileCatalogue.stockProfiles().first { it.mode == mode })

    private fun customProfile(
        id: String,
        name: String,
        mode: AppearanceMode,
    ): AppearanceProfile {
        val draft = stockDraft(mode)
        return AppearanceProfile(
            id = AppearanceProfileId(id),
            name = AppearanceProfileName(name),
            mode = mode,
            source = AppearanceProfileSource.CUSTOM,
            textFonts = draft.textFonts,
            textColors = draft.textColors,
            elementColors = draft.elementColors,
        )
    }

    private class LoadStore(
        private val loadResult: AppearanceProfileStoreLoadResult = AppearanceProfileStoreLoadResult.Missing,
        private val saveResult: AppearanceProfileStoreSaveResult = AppearanceProfileStoreSaveResult.Saved,
    ) : AppearanceProfileStore {
        val savedSnapshots = mutableListOf<AppearanceProfileStoreSnapshot>()

        override fun load(): AppearanceProfileStoreLoadResult =
            loadResult

        override fun save(snapshot: AppearanceProfileStoreSnapshot): AppearanceProfileStoreSaveResult {
            savedSnapshots += snapshot
            return saveResult
        }
    }
}
