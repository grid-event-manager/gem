package org.hostess.core.domain

@JvmInline
value class SecondLifeLoginUri(val value: String) {
    init {
        require(value.isNotBlank()) { "SecondLifeLoginUri cannot be blank." }
    }

    companion object {
        val SECOND_LIFE_DEFAULT: SecondLifeLoginUri =
            SecondLifeLoginUri("https://login.agni.lindenlab.com/cgi-bin/login.cgi")
    }
}

class SharedSecret private constructor(
    private val value: String,
) {
    fun revealForLogin(): String = value

    override fun toString(): String = "[redacted]"

    companion object {
        fun fromPlainText(value: String): SharedSecret? =
            value.takeIf { it.isNotBlank() }?.let(::SharedSecret)
    }
}

class LoginCredentialMaterial(
    val loginUri: SecondLifeLoginUri,
    val loginName: SecondLifeLoginName,
    val sharedSecret: SharedSecret,
    val startLocation: String?,
) {
    override fun toString(): String =
        "LoginCredentialMaterial(loginUri=$loginUri, loginName=$loginName, sharedSecret=[redacted], startLocation=$startLocation)"
}
