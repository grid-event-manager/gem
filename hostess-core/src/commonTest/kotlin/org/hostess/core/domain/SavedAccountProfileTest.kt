package org.hostess.core.domain

import org.hostess.core.ports.CredentialHandle
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs

class SavedAccountProfileTest {
    @Test
    fun `profile id requires v1 prefix and suffix`() {
        AccountProfileId("profile:v1:abc")

        assertFailsWith<IllegalArgumentException> {
            AccountProfileId("abc")
        }
        assertFailsWith<IllegalArgumentException> {
            AccountProfileId("profile:v1:")
        }
    }

    @Test
    fun `saved profile stores credential handle but no password`() {
        val profile = SavedAccountProfile(
            profileId = AccountProfileId("profile:v1:one"),
            loginName = loginName("jackraybold"),
            label = "jackraybold resident",
            credentialHandle = CredentialHandle("credential:v1:one"),
            startLocation = null,
        )

        val profileText = profile.toString()

        assertFalse(profileText.contains("password"), profileText)
        assertFalse(profileText.contains("sharedSecret"), profileText)
    }

    @Test
    fun `saved profile rejects blank label`() {
        assertFailsWith<IllegalArgumentException> {
            SavedAccountProfile(
                profileId = AccountProfileId("profile:v1:one"),
                loginName = loginName("jackraybold"),
                label = " ",
                credentialHandle = CredentialHandle("credential:v1:one"),
                startLocation = null,
            )
        }
    }

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName
}
