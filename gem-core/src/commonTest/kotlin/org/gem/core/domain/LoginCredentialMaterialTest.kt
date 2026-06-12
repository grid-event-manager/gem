package org.gem.core.domain

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoginCredentialMaterialTest {
    @Test
    fun `default login uri points to second life agni login`() {
        assertEquals(
            "https://login.agni.lindenlab.com/cgi-bin/login.cgi",
            SecondLifeLoginUri.SECOND_LIFE_DEFAULT.value,
        )
    }

    @Test
    fun `blank login uri is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            SecondLifeLoginUri(" ")
        }
    }

    @Test
    fun `shared secret rejects blank and redacts string form`() {
        assertNull(SharedSecret.fromPlainText(" "))

        val secret = assertNotNull(SharedSecret.fromPlainText("venue-password"))

        assertEquals("venue-password", secret.revealForLogin())
        assertEquals("[redacted]", secret.toString())
    }

    @Test
    fun `credential material is not a data class and redacts secret in string form`() {
        val material = LoginCredentialMaterial(
            loginUri = SecondLifeLoginUri.SECOND_LIFE_DEFAULT,
            loginName = loginName("venuehost"),
            sharedSecret = assertNotNull(SharedSecret.fromPlainText("venue-password")),
            startLocation = "uri:London City&76&174&23",
        )

        val materialText = material.toString()

        assertFalse(materialText.contains("venue-password"), materialText)
        assertContains(materialText, "sharedSecret=[redacted]")
        assertFalse(materialText.startsWith("LoginCredentialMaterial(") && "copy(" in materialText)
    }

    private fun loginName(input: String): SecondLifeLoginName =
        assertIs<SecondLifeLoginNameResult.Valid>(SecondLifeLoginName.fromUserInput(input)).loginName
}
