package org.hostess.protocol.libomv.runtime

import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoginPackageBuilderTest {
    @Test
    fun `builder emits canonical Hostess login package fields`() {
        val loginPackage = LoginPackageBuilder().build(
            secret = loginSecret(lastName = ""),
            viewerIdentity = viewerIdentity(),
            machineIdentity = machineIdentity(),
        ) ?: error("login package was not built")

        assertEquals("https://login.example/cgi-bin/login.cgi", loginPackage.loginUri)
        assertEquals("Venue", loginPackage.first)
        assertEquals("Resident", loginPackage.last)
        assertEquals("\$1\$8b2fee48cd255fddee9a662b55da4fd4", loginPackage.passwd)
        assertEquals("home", loginPackage.start)
        assertEquals("Hostess", loginPackage.channel)
        assertEquals("0.1.0.0", loginPackage.version)
        assertEquals("Linux 6.8.0 amd64 Test Runtime 17", loginPackage.platform)
        assertEquals("08:00:27:DC:4A:9E", loginPackage.mac)
        assertEquals("08:00:27:DC:4A:9E", loginPackage.id0)
        assertEquals("true", loginPackage.agreeToTos)
        assertEquals("true", loginPackage.readCritical)
        assertEquals(0, loginPackage.lastExecEvent)
        assertEquals(Duration.ofSeconds(120), loginPackage.timeout)
        assertEquals(
            listOf(
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
            ),
            loginPackage.options,
        )
    }

    @Test
    fun `builder rejects invalid shared secret without logging input`() {
        val loginPackage = LoginPackageBuilder().build(
            secret = loginSecret(sharedSecret = "\$1\$not-md5"),
            viewerIdentity = viewerIdentity(),
            machineIdentity = machineIdentity(),
        )

        assertNull(loginPackage)
    }

    private fun loginSecret(
        lastName: String = "Host",
        sharedSecret: String = "secret12",
    ): LoginSecret = LoginSecret(
        loginUri = "https://login.example/cgi-bin/login.cgi",
        firstName = "Venue",
        lastName = lastName,
        sharedSecret = sharedSecret,
        startLocation = "home",
    )

    private fun viewerIdentity(): HostessViewerIdentity = HostessViewerIdentity(
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
    )

    private fun machineIdentity(): HostessMachineIdentity = HostessMachineIdentity(
        mac = "08:00:27:DC:4A:9E",
        id0 = "08:00:27:DC:4A:9E",
    )
}
