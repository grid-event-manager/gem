package org.gem.core.ports

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialHandleTest {
    @Test
    fun `identifies Gem vault credential handles in one core-owned place`() {
        assertTrue(CredentialHandle("gem-vault:v1:abc").isGemVaultCredentialHandle())
        assertFalse(CredentialHandle("gem-vault:v1:").isGemVaultCredentialHandle())
        assertFalse(CredentialHandle("HOSTESS_SL_SECRET").isGemVaultCredentialHandle())
    }
}
