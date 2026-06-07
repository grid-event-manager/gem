package org.hostess.tools.cli.commands

import org.hostess.core.domain.AccountLabel
import org.hostess.core.ports.CredentialHandle
import org.hostess.core.ports.LoginRequest
import org.hostess.tools.cli.CommandArguments
import org.hostess.tools.cli.CommandMode

internal object CommandLoginRequest {
    fun from(arguments: CommandArguments, mode: CommandMode): LoginRequest? {
        val account = arguments.option("account") ?: if (mode == CommandMode.FAKE) "fake-account" else return null
        val credentialHandle = arguments.option("credential-env")
            ?: if (mode == CommandMode.FAKE) "fake-credential" else return null
        return LoginRequest(AccountLabel(account), CredentialHandle(credentialHandle))
    }
}
