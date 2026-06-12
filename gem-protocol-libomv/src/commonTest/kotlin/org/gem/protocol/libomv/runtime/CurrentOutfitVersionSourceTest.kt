package org.gem.protocol.libomv.runtime

import org.gem.protocol.libomv.LibomvSessionIdentity
import org.gem.protocol.libomv.mapping.LoginAppearanceState
import org.gem.protocol.libomv.mapping.LoginInventoryFolder
import org.gem.protocol.libomv.mapping.LoginInventoryRoots
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CurrentOutfitVersionSourceTest {
    @Test
    fun `single current outfit folder version wins over login cof version`() {
        val result = CurrentOutfitVersionSource().currentVersion(
            identity = identity(),
            roots = roots(folder(typeDefault = CurrentOutfitVersionSource.CURRENT_OUTFIT_FOLDER_TYPE, version = 7)),
            appearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 9),
        )

        assertEquals(7, assertIs<CurrentOutfitVersionResult.Available>(result).version)
    }

    @Test
    fun `login cof version is fallback when skeleton has no current outfit version`() {
        val result = CurrentOutfitVersionSource().currentVersion(
            identity = identity(),
            roots = roots(folder(typeDefault = 3, version = 7)),
            appearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 9),
        )

        assertEquals(9, assertIs<CurrentOutfitVersionResult.Available>(result).version)
    }

    @Test
    fun `multiple current outfit folders do not guess and use login fallback`() {
        val result = CurrentOutfitVersionSource().currentVersion(
            identity = identity(),
            roots = roots(
                folder(typeDefault = CurrentOutfitVersionSource.CURRENT_OUTFIT_FOLDER_TYPE, version = 7),
                folder(typeDefault = CurrentOutfitVersionSource.CURRENT_OUTFIT_FOLDER_TYPE, version = 8),
            ),
            appearanceState = LoginAppearanceState(agentAppearanceService = true, cofVersion = 9),
        )

        assertEquals(9, assertIs<CurrentOutfitVersionResult.Available>(result).version)
    }

    @Test
    fun `absent cof sources return redacted unavailable result`() {
        val result = CurrentOutfitVersionSource().currentVersion(
            identity = identity(),
            roots = roots(folder(typeDefault = CurrentOutfitVersionSource.CURRENT_OUTFIT_FOLDER_TYPE, version = -1)),
            appearanceState = LoginAppearanceState.empty(),
        )

        assertEquals(
            "cof version unavailable",
            assertIs<CurrentOutfitVersionResult.Unavailable>(result).redactedMessage,
        )
    }

    private fun roots(vararg folders: LoginInventoryFolder): LoginInventoryRoots = LoginInventoryRoots(
        inventoryRootId = "inventory-root-id",
        inventorySkeleton = folders.toList(),
        libraryRootId = null,
        libraryOwnerId = null,
        librarySkeleton = emptyList(),
    )

    private fun folder(typeDefault: Int, version: Int): LoginInventoryFolder = LoginInventoryFolder(
        folderId = "folder-$typeDefault-$version",
        parentId = "inventory-root-id",
        ownerId = "agent-id",
        name = "Folder",
        typeDefault = typeDefault,
        version = version,
    )

    private fun identity(): LibomvSessionIdentity = LibomvSessionIdentity(
        agentId = "agent-id",
        sessionId = "session-id",
        seedCapability = "https://caps.example/seed",
        simulatorIp = "203.0.113.8",
        simulatorPort = 13000,
        regionHandle = 123456789L,
        circuitCode = 987654321L,
    )
}
