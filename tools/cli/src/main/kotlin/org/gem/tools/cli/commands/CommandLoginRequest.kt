package org.gem.tools.cli.commands

import org.gem.core.domain.AccountLabel
import org.gem.core.ports.CredentialHandle
import org.gem.core.ports.LoginRequest
import org.gem.tools.cli.CommandArguments
import org.gem.tools.cli.CommandMode

internal object CommandLoginRequest {
    fun from(arguments: CommandArguments, mode: CommandMode): LoginRequest? {
        val account = arguments.option("account") ?: if (mode == CommandMode.FAKE) "fake-account" else return null
        val credentialHandle = arguments.option("credential-env")
            ?: if (mode == CommandMode.FAKE) "fake-credential" else return null
        return LoginRequest(AccountLabel(account), CredentialHandle(credentialHandle))
    }
}
