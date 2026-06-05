package org.hostess.protocol.libomv.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LoginPackageCaptureNormalizerTest {
    @Test
    fun `generated Hostess xml rpc package matches retained canonical shape with approved identity differences`() {
        val loginPackage = LoginPackageBuilder.build(
            secret = LoginSecret(
                loginUri = "https://login.example/cgi-bin/login.cgi",
                firstName = "Venue",
                lastName = "",
                sharedSecret = "secret12",
                startLocation = "home",
            ),
            viewerIdentity = HostessViewerIdentity(
                channel = "Hostess",
                version = "0.1.0.0",
                author = "Hostess",
                platform = HostessPlatformIdentity(
                    platform = "Linux",
                    platformVersion = "6.8.0",
                    platformString = "Linux 6.8.0 amd64 Test Runtime 17",
                ),
                host = HostessHostIdentity(
                    mac = "00000000000000000000000000000001",
                    id0 = "00000000000000000000000000000002",
                    hostId = "00000000000000000000000000000003",
                ),
            ),
            machineIdentity = HostessMachineIdentity(
                mac = "08:00:27:DC:4A:9E",
                id0 = "08:00:27:DC:4A:9E",
            ),
        ) ?: error("login package was not built")

        val normalized = LoginPackageCaptureNormalizer.normalize(
            LoginPackageSerializer.toXmlRpc(loginPackage),
        )

        assertEquals("login_to_simulator", normalized.methodName)
        assertEquals(CANONICAL_FIELD_NAMES, normalized.fields.keys)
        assertEquals(NormalizedLoginString("string", "Venue"), normalized.fields["first"])
        assertEquals(NormalizedLoginString("string", "Resident"), normalized.fields["last"])
        assertEquals(NormalizedLoginString("string", "home"), normalized.fields["start"])
        assertEquals(NormalizedLoginString("string", "Hostess"), normalized.fields["channel"])
        assertEquals(NormalizedLoginString("string", "0.1.0.0"), normalized.fields["version"])
        assertEquals(
            NormalizedLoginString("string", "Linux 6.8.0 amd64 Test Runtime 17"),
            normalized.fields["platform"],
        )
        assertEquals(NormalizedLoginString("string", "true"), normalized.fields["agree_to_tos"])
        assertEquals(NormalizedLoginString("string", "true"), normalized.fields["read_critical"])
        assertEquals(NormalizedLoginInteger("i4", 0), normalized.fields["last_exec_event"])
        assertEquals(NormalizedLoginStringArray("array<string>", CANONICAL_OPTIONS), normalized.fields["options"])
        assertTrue(
            assertIs<NormalizedLoginString>(normalized.fields["passwd"]).value.matches(SECOND_LIFE_HASH_PATTERN),
        )
        assertTrue(assertIs<NormalizedLoginString>(normalized.fields["mac"]).value.matches(RAW_MAC_PATTERN))
        assertTrue(assertIs<NormalizedLoginString>(normalized.fields["id0"]).value.matches(RAW_MAC_PATTERN))
    }

    private companion object {
        val CANONICAL_FIELD_NAMES = linkedSetOf(
            "first",
            "last",
            "passwd",
            "start",
            "channel",
            "version",
            "platform",
            "mac",
            "id0",
            "agree_to_tos",
            "read_critical",
            "last_exec_event",
            "options",
        )

        val CANONICAL_OPTIONS = listOf(
            "inventory-root",
            "inventory-skeleton",
            "inventory-lib-root",
            "inventory-lib-owner",
            "inventory-skel-lib",
            "initial-outfit",
            "gestures",
            "event_categories",
            "event_notifications",
            "classified_categories",
            "buddy-list",
            "ui-config",
            "tutorial_settings",
            "login-flags",
            "global-textures",
            "adult_compliant",
        )

        val SECOND_LIFE_HASH_PATTERN = Regex("\\$1\\$[0-9a-f]{32}")
        val RAW_MAC_PATTERN = Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5,}")
    }
}
