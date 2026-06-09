package org.hostess.credential.vault

interface VaultKeySource {
    fun getOrCreateKey(): VaultKeySourceResult

    fun deleteKey(): VaultKeySourceDeleteResult
}

sealed interface VaultKeySourceResult {
    data class Loaded(
        val keyMaterial: VaultKeyMaterial,
        val details: Set<String> = emptySet(),
    ) : VaultKeySourceResult

    data class KeySourceFailed(val message: String? = null) : VaultKeySourceResult
}

sealed interface VaultKeySourceDeleteResult {
    data object Deleted : VaultKeySourceDeleteResult
    data object Missing : VaultKeySourceDeleteResult
    data class KeySourceFailed(val message: String? = null) : VaultKeySourceDeleteResult
}
