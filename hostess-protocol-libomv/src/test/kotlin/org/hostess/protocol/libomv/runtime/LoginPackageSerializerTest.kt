package org.hostess.protocol.libomv.runtime

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginPackageSerializerTest {
    @Test
    fun `serializer emits xml rpc method call with field types and escaped values`() {
        val xml = LoginPackageSerializer.toXmlRpc(loginPackage())
        val normalized = LoginPackageCaptureNormalizer.normalize(xml)

        assertTrue(xml.startsWith("<?xml version=\"1.0\"?><methodCall>"))
        assertTrue(xml.contains("Ven&amp;ue"))
        assertTrue(xml.contains("Ho&lt;st"))
        assertTrue(xml.contains("Linux &apos;Box&apos; &amp; Test"))
        assertEquals("login_to_simulator", normalized.methodName)
        assertEquals(NormalizedLoginString("string", "Ven&ue"), normalized.fields["first"])
        assertEquals(NormalizedLoginString("string", "Ho<st"), normalized.fields["last"])
        assertEquals(NormalizedLoginString("string", "\$1\$8b2fee48cd255fddee9a662b55da4fd4"), normalized.fields["passwd"])
        assertEquals(NormalizedLoginInteger("i4", 0), normalized.fields["last_exec_event"])
        assertEquals(
            NormalizedLoginStringArray("array<string>", listOf("inventory-root", "a&b")),
            normalized.fields["options"],
        )
    }

    @Test
    fun `serializer omits non canonical legacy extras`() {
        val xml = LoginPackageSerializer.toXmlRpc(loginPackage())
        val normalized = LoginPackageCaptureNormalizer.normalize(xml)

        FORBIDDEN_FIELDS.forEach { field ->
            assertFalse("<name>$field</name>" in xml)
            assertFalse(field in normalized.fields)
        }
    }

    private fun loginPackage(): LoginPackage = LoginPackage(
        loginUri = "https://login.example/cgi-bin/login.cgi",
        first = "Ven&ue",
        last = "Ho<st",
        passwd = "\$1\$8b2fee48cd255fddee9a662b55da4fd4",
        start = "home",
        channel = "Hostess",
        version = "0.1.0.0",
        platform = "Linux 'Box' & Test",
        mac = "08:00:27:DC:4A:9E",
        id0 = "08:00:27:DC:4A:9E",
        agreeToTos = "true",
        readCritical = "true",
        lastExecEvent = 0,
        options = listOf("inventory-root", "a&b"),
        timeout = Duration.ofSeconds(120),
    )

    private companion object {
        val FORBIDDEN_FIELDS = listOf(
            "author",
            "viewer_digest",
            "user_agent",
            "platform_version",
            "platform_string",
            "host_id",
            "token",
            "extended_errors",
            "max-agent-groups",
        )
    }
}
