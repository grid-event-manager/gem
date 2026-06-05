package org.hostess.protocol.libomv.mapping

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class LibomvLoginMappingTest {
    @Test
    fun `classifies successful login response`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(successBody()),
        ).value

        assertEquals("live-session", success.sessionId.value)
    }

    @Test
    fun `classifies normal login failure without exposing source text`() {
        val failure = failureFor(LoginKeys.MESSAGE to "Bad password for Venue Host")

        assertEquals(LibomvLoginFailureKind.NORMAL_FAILURE, failure.kind)
        assertEquals("login failed", failure.redactedMessage)
        assertFalse(failure.redactedMessage.contains("Bad password"))
    }

    @Test
    fun `classifies terms of service challenge`() {
        val failure = failureFor(LoginKeys.MESSAGE to "Terms of Service acceptance requires agree_to_tos")

        assertEquals(LibomvLoginFailureKind.TOS_REQUIRED, failure.kind)
        assertEquals("login blocked: terms of service required", failure.redactedMessage)
        assertFalse(failure.redactedMessage.contains("agree_to_tos"))
    }

    @Test
    fun `classifies critical message challenge`() {
        val failure = failureFor(LoginKeys.REASON to "read_critical must be confirmed")

        assertEquals(LibomvLoginFailureKind.CRITICAL_MESSAGE_REQUIRED, failure.kind)
        assertEquals("login blocked: critical message required", failure.redactedMessage)
        assertFalse(failure.redactedMessage.contains("read_critical"))
    }

    @Test
    fun `classifies update challenge`() {
        val failure = failureFor(LoginKeys.ERROR to "mandatory viewer version update")

        assertEquals(LibomvLoginFailureKind.UPDATE_REQUIRED, failure.kind)
        assertEquals("login blocked: viewer update required", failure.redactedMessage)
        assertFalse(failure.redactedMessage.contains("mandatory"))
    }

    @Test
    fun `classifies mfa challenge`() {
        val failure = failureFor(LoginKeys.NEXT_URL to "authentication token required for multi-factor login")

        assertEquals(LibomvLoginFailureKind.MFA_REQUIRED, failure.kind)
        assertEquals("login blocked: mfa token required", failure.redactedMessage)
        assertFalse(failure.redactedMessage.contains("authentication token"))
    }

    @Test
    fun `classifies malformed response separately`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse("<llsd><map>".encodeToByteArray()),
        ).value

        assertEquals(LibomvLoginFailureKind.MALFORMED_RESPONSE, failure.kind)
        assertEquals("login response malformed", failure.redactedMessage)
    }

    @Test
    fun `classifies successful response with missing session as malformed`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "true")
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

        assertEquals(LibomvLoginFailureKind.MALFORMED_RESPONSE, failure.kind)
        assertEquals("login response malformed", failure.redactedMessage)
    }

    private fun failureFor(vararg fields: Pair<String, String>): LibomvLoginFailure =
        assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "false")
                    fields.forEach { (key, value) -> field(key, value) }
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

    private fun successBody(): ByteArray = buildString {
        append("<llsd><map>")
        field(LoginKeys.LOGIN, "true")
        field(LoginKeys.SESSION_ID, "live-session")
        append("</map></llsd>")
    }.encodeToByteArray()

    private fun StringBuilder.field(key: String, value: String) {
        append("<key>").append(key).append("</key><string>").append(value).append("</string>")
    }
}
