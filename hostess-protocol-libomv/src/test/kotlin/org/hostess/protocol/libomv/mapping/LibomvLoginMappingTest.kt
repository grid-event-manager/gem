package org.hostess.protocol.libomv.mapping

import kotlin.test.Test
import kotlin.test.assertContains
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
        assertEquals("login failed: message=Bad password for Venue Host; login=false", failure.redactedMessage)
    }

    @Test
    fun `classifies terms of service challenge`() {
        val failure = failureFor(LoginKeys.MESSAGE to "Terms of Service acceptance requires agree_to_tos")

        assertEquals(LibomvLoginFailureKind.TOS_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: terms of service required: " +
                "message=Terms of Service acceptance requires agree_to_tos; login=false",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies critical message challenge`() {
        val failure = failureFor(LoginKeys.REASON to "read_critical must be confirmed")

        assertEquals(LibomvLoginFailureKind.CRITICAL_MESSAGE_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: critical message required: reason=read_critical must be confirmed; login=false",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies update challenge`() {
        val failure = failureFor(LoginKeys.ERROR to "mandatory viewer version update")

        assertEquals(LibomvLoginFailureKind.UPDATE_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: viewer update required: error=mandatory viewer version update; login=false",
            failure.redactedMessage,
        )
    }

    @Test
    fun `classifies mfa challenge`() {
        val failure = failureFor(LoginKeys.NEXT_URL to "authentication token required for multi-factor login")

        assertEquals(LibomvLoginFailureKind.MFA_REQUIRED, failure.kind)
        assertEquals(
            "login blocked: mfa token required: login=false; " +
                "next_url=authentication token required for multi-factor login",
            failure.redactedMessage,
        )
    }

    @Test
    fun `redacts sensitive diagnostic material while preserving server message shape`() {
        val failure = failureFor(
            LoginKeys.MESSAGE to "Bad password for " +
                "12345678-1234-1234-1234-123456789abc token=secret https://login.example.invalid/path",
            LoginKeys.NEXT_URL to "https://login.example.invalid/mfa?token=secret",
        )

        assertContains(
            failure.redactedMessage,
            "message=Bad password for [redacted-id] token=[redacted] [redacted-url]",
        )
        assertContains(failure.redactedMessage, "next_url=[redacted-url]")
        assertFalse(failure.redactedMessage.contains("12345678-1234"))
        assertFalse(failure.redactedMessage.contains("login.example.invalid"))
        assertFalse(failure.redactedMessage.contains("token=secret"))
    }

    @Test
    fun `classifies malformed response separately`() {
        val failure = assertIs<LibomvLoginMappingResult.Failure>(
            LibomvLoginMapping.parse("<llsd><map>".encodeToByteArray()),
        ).value

        assertEquals(LibomvLoginFailureKind.MALFORMED_RESPONSE, failure.kind)
        assertEquals("login response malformed: response=<llsd><map>", failure.redactedMessage)
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
        assertEquals("login response malformed: missing=session_id", failure.redactedMessage)
    }

    @Test
    fun `classifies successful response with non scalar top level fields`() {
        val success = assertIs<LibomvLoginMappingResult.Success>(
            LibomvLoginMapping.parse(
                buildString {
                    append("<llsd><map>")
                    field(LoginKeys.LOGIN, "true")
                    field(LoginKeys.SESSION_ID, "live-session")
                    append("<key>inventory-skeleton</key><array><string>ignored</string></array>")
                    append("</map></llsd>")
                }.encodeToByteArray(),
            ),
        ).value

        assertEquals("live-session", success.sessionId.value)
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
