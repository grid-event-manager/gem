package org.hostess.core.ports

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CredentialHandleTest {
    @Test
    fun `identifies Hostess vault credential handles in one core-owned place`() {
        assertTrue(CredentialHandle("hostess-vault:v1:abc").isHostessVaultCredentialHandle())
        assertFalse(CredentialHandle("hostess-vault:v1:").isHostessVaultCredentialHandle())
        assertFalse(CredentialHandle("HOSTESS_SL_SECRET").isHostessVaultCredentialHandle())
    }
}
