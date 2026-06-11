package org.hostess.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SecondLifeLoginNameTest {
    @Test
    fun `normalizes one-part names to resident legacy name`() {
        val result = assertIs<SecondLifeLoginNameResult.Valid>(
            SecondLifeLoginName.fromUserInput("  VenueHost  "),
        )

        assertEquals("venuehost resident", result.loginName.value)
        assertEquals("venuehost", result.loginName.firstName)
        assertEquals("resident", result.loginName.lastName)
    }

    @Test
    fun `normalizes dotted and whitespace names`() {
        val result = assertIs<SecondLifeLoginNameResult.Valid>(
            SecondLifeLoginName.fromUserInput("VenueHost.Resident"),
        )

        assertEquals("venuehost resident", result.loginName.value)
    }

    @Test
    fun `keeps multipart last name text after first token`() {
        val result = assertIs<SecondLifeLoginNameResult.Valid>(
            SecondLifeLoginName.fromUserInput("  Event   Venue   Host "),
        )

        assertEquals("event venue host", result.loginName.value)
        assertEquals("event", result.loginName.firstName)
        assertEquals("venue host", result.loginName.lastName)
    }

    @Test
    fun `rejects blank input`() {
        val result = assertIs<SecondLifeLoginNameResult.Invalid>(
            SecondLifeLoginName.fromUserInput("   "),
        )

        assertEquals(SecondLifeLoginNameInvalidReason.BLANK, result.reason)
    }
}
