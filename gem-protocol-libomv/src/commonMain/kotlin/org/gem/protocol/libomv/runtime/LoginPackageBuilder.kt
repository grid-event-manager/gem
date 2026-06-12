package org.gem.protocol.libomv.runtime

import org.gem.core.domain.GemDelay

internal class LoginPackageBuilder(
    private val digestPort: Md5DigestPort,
) {
    fun build(
        secret: LoginSecret,
        viewerIdentity: GemViewerIdentity,
        machineIdentity: GemMachineIdentity,
    ): LoginPackage? {
        val passwordHash = SecondLifePasswordHash.fromSharedSecret(secret.sharedSecret, digestPort) ?: return null
        return LoginPackage(
            loginUri = secret.loginUri,
            first = secret.firstName,
            last = secret.lastName.ifBlank { RESIDENT_LAST_NAME },
            passwd = passwordHash.wireValue,
            start = secret.startLocation,
            channel = viewerIdentity.channel,
            version = viewerIdentity.version,
            platform = viewerIdentity.platform.platformString,
            mac = machineIdentity.mac,
            id0 = machineIdentity.id0,
            agreeToTos = TRUE_STRING,
            readCritical = TRUE_STRING,
            lastExecEvent = 0,
            options = DEFAULT_OPTIONS,
            timeout = LOGIN_TIMEOUT,
        )
    }

    private companion object {
        val LOGIN_TIMEOUT: GemDelay = GemDelay.ofSeconds(120)
        const val RESIDENT_LAST_NAME = "Resident"
        const val TRUE_STRING = "true"

        val DEFAULT_OPTIONS = listOf(
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
    }
}
