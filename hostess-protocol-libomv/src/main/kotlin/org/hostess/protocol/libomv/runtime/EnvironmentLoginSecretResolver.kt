package org.hostess.protocol.libomv.runtime

import org.hostess.core.ports.CredentialHandle

class EnvironmentLoginSecretResolver(
    private val getenv: (String) -> String? = System::getenv,
) : LoginSecretResolver {
    override fun resolve(handle: CredentialHandle): LoginSecret? {
        val envName = handle.value.takeIf(::isEnvName) ?: return null
        val raw = getenv(envName)?.takeIf(String::isNotBlank) ?: return null
        return LoginSecretJsonDecoder.decode(raw)
    }

    private fun isEnvName(value: String): Boolean =
        value.matches(ENV_NAME_REGEX)

    private companion object {
        val ENV_NAME_REGEX: Regex = Regex("[A-Za-z_][A-Za-z0-9_]*")
    }
}
