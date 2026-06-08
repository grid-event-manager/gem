package org.hostess.protocol.libomv.runtime

import org.hostess.protocol.libomv.LibomvSessionIdentity
import org.hostess.protocol.libomv.mapping.LoginAppearanceState
import org.hostess.protocol.libomv.mapping.LoginInventoryRoots

internal object CurrentOutfitVersionSource {
    fun currentVersion(
        identity: LibomvSessionIdentity,
        roots: LoginInventoryRoots,
        appearanceState: LoginAppearanceState,
    ): CurrentOutfitVersionResult {
        val skeletonVersion = roots.inventorySkeleton
            .singleOrNull { it.typeDefault == CURRENT_OUTFIT_FOLDER_TYPE }
            ?.version
            ?.takeIf { it >= 0 }
        val version = skeletonVersion ?: appearanceState.cofVersion?.takeIf { it >= 0 }
        return version
            ?.let(CurrentOutfitVersionResult::Available)
            ?: CurrentOutfitVersionResult.Unavailable("cof version unavailable")
    }

    const val CURRENT_OUTFIT_FOLDER_TYPE: Int = 46
}

internal sealed interface CurrentOutfitVersionResult {
    data class Available(val version: Int) : CurrentOutfitVersionResult
    data class Unavailable(val redactedMessage: String) : CurrentOutfitVersionResult
}
